package com.roomfit.client;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
