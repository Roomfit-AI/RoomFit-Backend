package com.roomfit.room.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomFurnitureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void updateFurnitureStatus_returnsUpdatedRoom() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furnitureUpdates": [
                                    { "id": "bed-1", "status": "EXISTING" },
                                    { "id": "desk-1", "status": "DELETED" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                // roomId 1은 공유 샘플 템플릿이라 게스트별 소유권 분리 이후로는
                // 그 게스트 개인 fork의 id로 바뀐다(RoomAccessService 참고).
                .andExpect(jsonPath("$.data.roomId", notNullValue()))
                .andExpect(jsonPath("$.data.furniture[?(@.id == 'bed-1')].status").value(hasItems("EXISTING")))
                .andExpect(jsonPath("$.data.furniture[?(@.id == 'desk-1')].status").value(hasItems("DELETED")));
    }

    @Test
    void updateFurnitureStatus_withUnknownRoom_returnsRoomNotFound() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furnitureUpdates": [
                                    { "id": "desk-1", "status": "DELETED" }
                                  ]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void updateFurnitureStatus_withUnknownFurniture_returnsFurnitureNotFound() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furnitureUpdates": [
                                    { "id": "ghost-1", "status": "DELETED" }
                                  ]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("FURNITURE_NOT_FOUND"));
    }

    @Test
    void updateFurnitureStatus_withMissingFurnitureUpdates_returnsInvalidRequestBody() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furniture": [
                                    { "id": "desk-1", "status": "DELETED" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void updateFurnitureStatus_withEmptyFurnitureUpdates_returnsInvalidRequestBody() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furnitureUpdates": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void updateFurnitureStatus_withBlankItemField_returnsInvalidRequestBody() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furnitureUpdates": [
                                    { "id": " ", "status": "DELETED" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void updateFurnitureStatus_withLowercaseStatus_returnsInvalidFurnitureStatus() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furnitureUpdates": [
                                    { "id": "desk-1", "status": "deleted" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_STATUS"));
    }
}
