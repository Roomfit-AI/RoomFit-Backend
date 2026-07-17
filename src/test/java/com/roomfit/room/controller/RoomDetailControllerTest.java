package com.roomfit.room.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoomDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRoom_returnsRoomJson() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                // roomId 1은 공유 샘플 템플릿이라 게스트별 소유권 분리 이후로는
                // 그 게스트 개인 fork의 id로 바뀐다(RoomAccessService 참고) —
                // 정확한 값 대신 존재만 확인, 내용(치수/가구)은 그대로 복사되므로 계속 검증.
                .andExpect(jsonPath("$.data.roomId", notNullValue()))
                .andExpect(jsonPath("$.data.room.width").value(5.8))
                .andExpect(jsonPath("$.data.room.depth").value(5.4))
                .andExpect(jsonPath("$.data.room.height").value(2.7))
                .andExpect(jsonPath("$.data.room.unit").value("meter"))
                .andExpect(jsonPath("$.data.openings[*].type").value(hasItems("door", "window")))
                .andExpect(jsonPath("$.data.furniture").value(hasSize(4)))
                .andExpect(jsonPath("$.data.furniture[*].id").value(hasItems(
                        "bed-1", "desk-1", "chair-1", "wardrobe-1"
                )))
                .andExpect(jsonPath("$.data.furniture[*].status").value(hasItems("EXISTING")));
    }

    @Test
    void getRoom_withUnknownRoom_returnsRoomNotFound() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }
}
