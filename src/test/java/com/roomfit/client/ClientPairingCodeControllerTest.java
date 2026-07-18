package com.roomfit.client;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "roomfit.client-scope.enabled=true",
        "roomfit.client-scope.required=false",
        "roomfit.llm.feedback.enabled=false",
        "roomfit.llm.api-key=",
        "roomfit.llm.base-url=",
        "roomfit.llm.model="
})
@AutoConfigureMockMvc
class ClientPairingCodeControllerTest {

    private static final String CLIENT_A = "0c1c4a96-7f5b-48b7-9a01-333333333333";
    private static final String CLIENT_B = "0c1c4a96-7f5b-48b7-9a01-444444444444";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issueOrGetCode_isIdempotentPerClientAndDistinctAcrossClients() throws Exception {
        String codeA1 = issueCode(CLIENT_A);
        String codeA2 = issueCode(CLIENT_A);
        String codeB = issueCode(CLIENT_B);

        assertThat(codeA1).isEqualTo(codeA2);
        assertThat(codeA1).isNotEqualTo(codeB);
        assertThat(codeA1).hasSize(8);
    }

    // 실제로 앱에서 재현된 버그: 같은(아직 코드가 없는) clientId로 거의 동시에
    // 두 요청이 들어오면(화면 재진입 등으로 fetch가 겹치는 경우), "조회 후 없으면
    // 생성"이 원자적이지 않아 하나는 client_id unique 제약 위반으로 500이 났었다.
    // 지금은 그 경합을 잡아 먼저 이긴 쪽 코드를 재사용하므로 둘 다 200과 같은
    // 코드를 받아야 한다.
    @Test
    void issueOrGetCode_underConcurrentFirstRequests_neverFailsAndConvergesOnOneCode() throws Exception {
        String brandNewClientId = "0c1c4a96-7f5b-48b7-9a01-555555555555";
        int concurrency = 8;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startLine = new CountDownLatch(1);

        try {
            Callable<String> task = () -> {
                startLine.await();
                return issueCode(brandNewClientId);
            };
            List<Future<String>> futures = IntStream.range(0, concurrency)
                    .mapToObj(ignored -> executor.submit(task))
                    .collect(Collectors.toList());

            // 전부 제출을 마친 뒤에야 동시에 풀어준다 — invokeAll처럼 제출 시점에
            // 이미 실행이 끝나버려 "동시성"이 사라지는 걸 피하기 위해 submit +
            // 래치 조합을 쓴다.
            startLine.countDown();

            Set<String> distinctCodes = new HashSet<>();
            for (Future<String> future : futures) {
                distinctCodes.add(future.get());
            }

            assertThat(distinctCodes).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void issueOrGetCode_withoutClientIdHeader_returnsClientIdRequired() throws Exception {
        mockMvc.perform(post("/api/clients/pairing-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ID_REQUIRED"));
    }

    @Test
    void redeem_returnsOriginalClientId_andIgnoresDashesSpacingAndCase() throws Exception {
        String code = issueCode(CLIENT_A);
        String formatted = code.substring(0, 4) + "-" + code.substring(4);

        redeemExpectingClientId(code, CLIENT_A);
        redeemExpectingClientId(formatted.toLowerCase(), CLIENT_A);
        redeemExpectingClientId(" " + code + " ", CLIENT_A);
    }

    @Test
    void redeem_withUnknownCode_returnsPairingCodeNotFound() throws Exception {
        mockMvc.perform(post("/api/clients/pairing-code/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ZZZZZZZZ\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAIRING_CODE_NOT_FOUND"));
    }

    @Test
    void redeem_doesNotRequireClientIdHeader() throws Exception {
        String code = issueCode(CLIENT_A);

        // 아직 신원을 모르는(=처음 페어링하는) 브라우저를 흉내내는 호출이라
        // X-RoomFit-Client-Id 헤더를 일부러 붙이지 않는다.
        mockMvc.perform(post("/api/clients/pairing-code/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(CLIENT_A));
    }

    @Test
    void regenerateCode_invalidatesOldCodeAndIssuesNewOne() throws Exception {
        String oldCode = issueCode(CLIENT_A);
        String newCode = regenerateCode(CLIENT_A);

        assertThat(newCode).isNotEqualTo(oldCode);

        mockMvc.perform(post("/api/clients/pairing-code/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(oldCode)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAIRING_CODE_NOT_FOUND"));
        redeemExpectingClientId(newCode, CLIENT_A);

        // 재발급 이후 코드 조회는 새 코드를 그대로 돌려준다(멱등 유지).
        assertThat(issueCode(CLIENT_A)).isEqualTo(newCode);
    }

    @Test
    void regenerateCode_withoutClientIdHeader_returnsClientIdRequired() throws Exception {
        mockMvc.perform(post("/api/clients/pairing-code/regenerate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ID_REQUIRED"));
    }

    private String issueCode(String clientId) throws Exception {
        String response = mockMvc.perform(post("/api/clients/pairing-code")
                        .header(ClientScopeService.HEADER_NAME, clientId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.code");
    }

    private String regenerateCode(String clientId) throws Exception {
        String response = mockMvc.perform(post("/api/clients/pairing-code/regenerate")
                        .header(ClientScopeService.HEADER_NAME, clientId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.code");
    }

    private void redeemExpectingClientId(String code, String expectedClientId) throws Exception {
        mockMvc.perform(post("/api/clients/pairing-code/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(expectedClientId));
    }
}
