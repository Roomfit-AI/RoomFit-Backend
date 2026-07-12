package com.roomfit.room.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoomDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deleteUploadedRoom_withRoomPlanRoom_deletesRoomAndRemovesItFromRecentUploads() throws Exception {
        Integer roomId = uploadRoom("Room to delete");

        mockMvc.perform(delete("/api/rooms/uploads/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        mockMvc.perform(get("/api/rooms/uploads/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].roomId", not(hasItem(roomId))));
    }

    @Test
    void deleteUploadedRoom_withUnknownRoom_returnsRoomNotFound() throws Exception {
        mockMvc.perform(delete("/api/rooms/uploads/{roomId}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void deleteUploadedRoom_withSampleRoom_returnsDeleteNotAllowedAndKeepsRoom() throws Exception {
        mockMvc.perform(delete("/api/rooms/uploads/{roomId}", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_DELETE_NOT_ALLOWED"));

        mockMvc.perform(get("/api/rooms/{roomId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("SAMPLE"));
    }

    private Integer uploadRoom(String name) throws Exception {
        String response = mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "room": {
                                    "width": 3.2,
                                    "depth": 4.5,
                                    "height": 2.4
                                  }
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.data.roomId");
    }
}
