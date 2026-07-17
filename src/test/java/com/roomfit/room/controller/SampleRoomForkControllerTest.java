package com.roomfit.room.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 요구사항 "샘플룸도 게스트별로 복사본을 만들어 분리"를 검증한다 —
 * RoomAccessService.resolveAccessibleRoom 참고. 공유 템플릿(ownerId=null)
 * 자체는 절대 바뀌지 않고, 게스트가 처음 쓰기 작업을 하는 순간 개인 fork가
 * 생기며, 같은 게스트의 재요청은 항상 같은 fork를 재사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SampleRoomForkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sampleTemplate_forksIndependentlyPerGuest_andReusesOwnForkOnRepeat() throws Exception {
        String tokenA = issueGuestToken();
        String tokenB = issueGuestToken();
        Long templateId = firstSampleTemplateId(tokenA);

        Long forkA = getRoomId(templateId, tokenA);
        Long forkAAgain = getRoomId(templateId, tokenA);
        Long forkB = getRoomId(templateId, tokenB);

        assertThat(forkA).isNotEqualTo(templateId);
        assertThat(forkAAgain).isEqualTo(forkA); // 같은 게스트의 재요청 = 같은 fork 재사용
        assertThat(forkB).isNotEqualTo(templateId);
        assertThat(forkB).isNotEqualTo(forkA); // 다른 게스트는 다른 fork

        // A가 자기 fork의 가구를 지워도(여전히 원본 템플릿 id로 요청함 — 프론트가
        // fork를 모른 채 계속 원본 id를 참조하는 실제 상황과 동일) B의 fork에는
        // 영향이 없어야 한다.
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", templateId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "furnitureUpdates": [ { "id": "bed-1", "status": "DELETED" } ] }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/rooms/{roomId}", forkB).header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture[?(@.id == 'bed-1')].status").value(hasItems("EXISTING")));

        mockMvc.perform(get("/api/rooms/{roomId}", forkA).header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture[?(@.id == 'bed-1')].status").value(hasItems("DELETED")));
    }

    @Test
    void sampleList_neverShowsForks_evenAfterManyGuestsFork() throws Exception {
        String tokenViewer = issueGuestToken();
        String samplesBefore = mockMvc.perform(get("/api/rooms/samples").header("Authorization", tokenViewer))
                .andReturn().getResponse().getContentAsString();
        List<Integer> roomIdsBefore = JsonPath.read(samplesBefore, "$.data[*].roomId");
        Long templateId = firstSampleTemplateId(tokenViewer);

        // 서로 다른 게스트 세 명이 같은 템플릿을 fork하게 만든다.
        for (int i = 0; i < 3; i++) {
            getRoomId(templateId, issueGuestToken());
        }

        String samplesAfter = mockMvc.perform(get("/api/rooms/samples").header("Authorization", tokenViewer))
                .andReturn().getResponse().getContentAsString();
        List<Integer> roomIdsAfter = JsonPath.read(samplesAfter, "$.data[*].roomId");

        assertThat(roomIdsAfter).containsExactlyInAnyOrderElementsOf(roomIdsBefore);
    }

    private String issueGuestToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/guest"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(response, "$.data.token");
    }

    private Long firstSampleTemplateId(String bearerToken) throws Exception {
        String response = mockMvc.perform(get("/api/rooms/samples").header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer roomId = JsonPath.read(response, "$.data[0].roomId");
        return roomId.longValue();
    }

    private Long getRoomId(Long roomId, String bearerToken) throws Exception {
        String response = mockMvc.perform(get("/api/rooms/{roomId}", roomId).header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer resolvedId = JsonPath.read(response, "$.data.roomId");
        return resolvedId.longValue();
    }
}
