package com.roomfit.room.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadRoom_savesRoomPlanRoomAndGetRoomReturnsSameShape() throws Exception {
        String uploadResponse = mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "RoomPlan Scan Room",
                                  "room": {
                                    "width": 3.2,
                                    "depth": 4.5,
                                    "height": 2.4
                                  },
                                  "openings": [
                                    {
                                      "id": "door-1",
                                      "type": "door",
                                      "wall": "south",
                                      "offset": 0.7,
                                      "width": 0.8,
                                      "height": 2.1
                                    },
                                    {
                                      "id": "window-1",
                                      "type": "window",
                                      "wall": "north",
                                      "offset": 1.5,
                                      "width": 1.2,
                                      "height": 1.0,
                                      "sillHeight": 0.9
                                    }
                                  ],
                                  "furniture": [
                                    {
                                      "id": "bed-1",
                                      "type": "bed",
                                      "label": "침대",
                                      "width": 1.1,
                                      "depth": 2.0,
                                      "height": 0.45,
                                      "position": {
                                        "x": 1.2,
                                        "z": 1.4
                                      },
                                      "rotation": 88
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.roomId", greaterThan(1)))
                .andExpect(jsonPath("$.data.name").value("RoomPlan Scan Room"))
                .andExpect(jsonPath("$.data.room.unit").value("meter"))
                .andExpect(jsonPath("$.data.openings[*].type").value(hasItems("door", "window")))
                .andExpect(jsonPath("$.data.furniture[0].status").value("EXISTING"))
                .andExpect(jsonPath("$.data.furniture[0].rotation").value(90.0))
                .andExpect(jsonPath("$.data.source").value("ROOMPLAN"))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer roomId = JsonPath.read(uploadResponse, "$.data.roomId");

        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(roomId))
                .andExpect(jsonPath("$.data.name").value("RoomPlan Scan Room"))
                .andExpect(jsonPath("$.data.source").value("ROOMPLAN"))
                .andExpect(jsonPath("$.data.furniture[0].rotation").value(90.0));

        mockMvc.perform(get("/api/rooms/samples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].source", everyItem(org.hamcrest.Matchers.is("SAMPLE"))));
    }

    @Test
    void uploadRoom_withoutNameAndArrays_usesDefaults() throws Exception {
        mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "room": {
                                    "width": 2.5,
                                    "depth": 3.5,
                                    "height": 2.4
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Uploaded Room"))
                .andExpect(jsonPath("$.data.room.unit").value("meter"))
                .andExpect(jsonPath("$.data.openings.length()").value(0))
                .andExpect(jsonPath("$.data.furniture.length()").value(0));
    }

    @Test
    void uploadRoom_createsNewRoomWithoutMutatingAnExistingRoom() throws Exception {
        String first = mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Room A", "room": { "width": 3.1, "depth": 4.2, "height": 2.4 } }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer roomAId = JsonPath.read(first, "$.data.roomId");

        String second = mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Room B", "room": { "width": 5.2, "depth": 6.3, "height": 2.8 } }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer roomBId = JsonPath.read(second, "$.data.roomId");

        org.assertj.core.api.Assertions.assertThat(roomBId).isNotEqualTo(roomAId);
        mockMvc.perform(get("/api/rooms/{roomId}", roomAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Room A"))
                .andExpect(jsonPath("$.data.room.width").value(3.1))
                .andExpect(jsonPath("$.data.room.depth").value(4.2));
        mockMvc.perform(get("/api/rooms/{roomId}", roomBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Room B"))
                .andExpect(jsonPath("$.data.room.width").value(5.2));
    }

    @Test
    void uploadRoom_withInvalidRoomDimension_returnsInvalidRoomDimension() throws Exception {
        mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "room": {
                                    "width": 0,
                                    "depth": 4.5,
                                    "height": 2.4
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_ROOM_DIMENSION"));
    }

    @Test
    void uploadRoom_withFurnitureOutsideRoom_repositionsItDuringRoomPlanImport() throws Exception {
        mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "room": {
                                    "width": 3.2,
                                    "depth": 4.5,
                                    "height": 2.4
                                  },
                                  "furniture": [
                                    {
                                      "id": "bed-1",
                                      "type": "bed",
                                      "width": 1.1,
                                      "depth": 2.0,
                                      "height": 0.45,
                                      "position": {
                                        "x": 0.2,
                                        "z": 1.4
                                      }
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importStatus").value("ACCEPTED_WITH_WARNINGS"))
                .andExpect(jsonPath("$.data.importWarnings[*].code", hasItems("FURNITURE_REPOSITIONED")));
    }

    @Test
    void uploadRoom_withRotatedCornerOutsideRoom_repositionsItDuringRoomPlanImport() throws Exception {
        mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "room": {
                                    "width": 3.2,
                                    "depth": 4.5,
                                    "height": 2.4
                                  },
                                  "furniture": [
                                    {
                                      "id": "bed-1",
                                      "type": "bed",
                                      "width": 1.1,
                                      "depth": 2.0,
                                      "height": 0.45,
                                      "position": { "x": 0.8, "z": 1.4 },
                                      "rotation": 90
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importStatus").value("ACCEPTED_WITH_WARNINGS"))
                .andExpect(jsonPath("$.data.importWarnings[*].code", hasItems("FURNITURE_REPOSITIONED")));
    }
}
