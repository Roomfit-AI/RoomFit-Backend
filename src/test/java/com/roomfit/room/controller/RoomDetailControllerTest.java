package com.roomfit.room.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
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
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.room.width").value(3.2))
                .andExpect(jsonPath("$.data.room.depth").value(4.5))
                .andExpect(jsonPath("$.data.room.height").value(2.4))
                .andExpect(jsonPath("$.data.room.unit").value("meter"))
                .andExpect(jsonPath("$.data.openings[*].type").value(hasItems("door", "window")))
                .andExpect(jsonPath("$.data.furniture[*].id").value(hasItems("bed-1", "desk-1", "wardrobe-1")))
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
