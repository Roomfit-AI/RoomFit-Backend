package com.roomfit.room.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 요구사항의 "사용자 A와 B의 게스트 토큰 생성 → 방 목록 분리 → 서로의 room에
 * 접근 불가"를 그대로 재현한다. Room, RoomPlan 업로드/최근 업로드뿐 아니라
 * Agent Context/Layout 추천/피드백/확정까지 전부 room 소유권 검사를
 * 거치므로(RoomAccessService.resolveAccessibleRoom), B가 A의 contextId나
 * layoutId를 직접 안다고 해도(이 MVP는 auto-increment id라 추측 가능) 그
 * 너머의 room 소유권 검사에서 막힌다는 것까지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomOwnershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void twoGuests_haveSeparateUploadedRoomLists() throws Exception {
        String tokenA = issueGuestToken();
        String tokenB = issueGuestToken();

        Long roomIdA = uploadRoom(tokenA, "A의 방");
        Long roomIdB = uploadRoom(tokenB, "B의 방");

        mockMvc.perform(get("/api/rooms/uploads/recent").header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].roomId").value(roomIdA));

        mockMvc.perform(get("/api/rooms/uploads/recent").header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].roomId").value(roomIdB));
    }

    @Test
    void otherGuestsRoom_getUpdateDelete_allReturnNotFound() throws Exception {
        String tokenA = issueGuestToken();
        String tokenB = issueGuestToken();
        Long roomIdB = uploadRoom(tokenB, "B의 방");

        mockMvc.perform(get("/api/rooms/{roomId}", roomIdB).header("Authorization", tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(put("/api/rooms/{roomId}/furniture", roomIdB)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "furnitureUpdates": [ { "id": "desk-1", "status": "DELETED" } ] }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(put("/api/rooms/{roomId}/layout", roomIdB)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "furniture": [] }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(delete("/api/rooms/uploads/{roomId}", roomIdB).header("Authorization", tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        // A의 시도가 전부 막힌 뒤에도 B는 자기 방을 여전히 정상 조회할 수 있어야 한다
        // (A의 실패한 시도가 B의 방에 어떤 영향도 주지 않았는지 확인).
        mockMvc.perform(get("/api/rooms/{roomId}", roomIdB).header("Authorization", tokenB))
                .andExpect(status().isOk());
    }

    @Test
    void otherGuestsRoom_agentContextCreation_returnsNotFound() throws Exception {
        String tokenA = issueGuestToken();
        String tokenB = issueGuestToken();
        Long roomIdB = uploadRoom(tokenB, "B의 방");

        mockMvc.perform(post("/api/agent/context")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": %d,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "selectedImageIds": [1]
                                }
                                """.formatted(roomIdB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void otherGuestsContextOrLayout_recommendUpdateFeedbackConfirm_allReturnNotFound() throws Exception {
        String tokenA = issueGuestToken();
        String tokenB = issueGuestToken();

        Long roomIdA = uploadRoom(tokenA, "A의 방");
        Long contextIdA = createContext(tokenA, roomIdA);
        Long layoutIdA = recommend(tokenA, contextIdA);

        // B가 A의 contextId/layoutId를 안다고 해도(순차 id라 추측 가능) 그
        // 너머의 room 소유권 검사에서 전부 막혀야 한다.
        mockMvc.perform(post("/api/layouts/recommend")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contextId\": %d }".formatted(contextIdA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(put("/api/layouts/{layoutId}", layoutIdA)
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furniture": [
                                    { "id": "desk-1", "position": { "x": 1.0, "z": 1.0 }, "rotation": 0, "status": "EXISTING" }
                                  ]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(post("/api/layouts/feedback")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"layoutId\": %d, \"feedback\": \"책상 더 크게\" }".formatted(layoutIdA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutIdA).header("Authorization", tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        // A 본인은 여전히 정상적으로 확정할 수 있어야 한다.
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutIdA).header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));
    }

    private String issueGuestToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/guest"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(response, "$.data.token");
    }

    private Long uploadRoom(String bearerToken, String name) throws Exception {
        String response = mockMvc.perform(post("/api/rooms/upload")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "room": { "width": 3.0, "depth": 3.0, "height": 2.4, "unit": "meter" },
                                  "openings": [],
                                  "furniture": [
                                    { "id": "desk-1", "type": "desk", "label": "책상", "width": 1.0, "depth": 0.6, "height": 0.72,
                                      "position": { "x": 1.0, "z": 1.0 }, "rotation": 0, "status": "EXISTING" }
                                  ]
                                }
                                """.formatted(name))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer roomId = JsonPath.read(response, "$.data.roomId");
        return roomId.longValue();
    }

    private Long createContext(String bearerToken, Long roomId) throws Exception {
        String response = mockMvc.perform(post("/api/agent/context")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": %d,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "selectedImageIds": [1]
                                }
                                """.formatted(roomId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer contextId = JsonPath.read(response, "$.data.contextId");
        return contextId.longValue();
    }

    private Long recommend(String bearerToken, Long contextId) throws Exception {
        String response = mockMvc.perform(post("/api/layouts/recommend")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contextId\": %d }".formatted(contextId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer layoutId = JsonPath.read(response, "$.data.layoutId");
        return layoutId.longValue();
    }
}
