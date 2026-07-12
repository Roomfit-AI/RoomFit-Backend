package com.roomfit.room.controller;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoomRecentUploadsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void getRecentUploadedRooms_withoutUploads_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/rooms/uploads/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.error").value((Object) null));
    }

    @Test
    @Order(2)
    void getRecentUploadedRooms_returnsRoomPlanRoomsNewestFirstAndAppliesLimit() throws Exception {
        uploadRoom("First upload");
        uploadRoom("Second upload");
        uploadRoom("Latest upload");

        mockMvc.perform(get("/api/rooms/uploads/recent").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Latest upload"))
                .andExpect(jsonPath("$.data[1].name").value("Second upload"))
                .andExpect(jsonPath("$.data[*].source", everyItem(is("ROOMPLAN"))));
    }

    private void uploadRoom(String name) throws Exception {
        mockMvc.perform(post("/api/rooms/upload")
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
                .andExpect(status().isCreated());
    }
}
