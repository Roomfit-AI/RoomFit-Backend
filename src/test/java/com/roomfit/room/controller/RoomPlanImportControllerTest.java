package com.roomfit.room.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoomPlanImportControllerTest {

    private static final String CLIENT_A = "08f5dcbd-b499-4d96-8b3b-111111111111";
    private static final String CLIENT_B = "08f5dcbd-b499-4d96-8b3b-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scanGeometryWithSmallBoundaryDriftZeroThicknessAndExistingCollision_isImportedWithWarnings() throws Exception {
        String response = mockMvc.perform(post("/api/rooms/upload")
                        .header("X-RoomFit-Client-Id", CLIENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scanFixture()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importStatus").value("ACCEPTED_WITH_WARNINGS"))
                .andExpect(jsonPath("$.data.importWarnings[*].code", hasItem("ROOM_WALL_DIMENSION_NORMALIZED")))
                .andExpect(jsonPath("$.data.importWarnings[*].code", hasItem("ZERO_WALL_THICKNESS_ACCEPTED")))
                .andExpect(jsonPath("$.data.importWarnings[*].code", hasItem("FURNITURE_REPOSITIONED")))
                .andExpect(jsonPath("$.data.importWarnings[?(@.code == 'FURNITURE_REPOSITIONED')][0].adjustmentMeters",
                        notNullValue()))
                .andReturn().getResponse().getContentAsString();

        int roomId = ((Number) JsonPath.read(response, "$.data.roomId")).intValue();
        mockMvc.perform(get("/api/rooms/{roomId}", roomId).header("X-RoomFit-Client-Id", CLIENT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importWarnings[*].code", hasItem("FURNITURE_REPOSITIONED")));
        mockMvc.perform(get("/api/rooms/{roomId}", roomId).header("X-RoomFit-Client-Id", CLIENT_B))
                .andExpect(status().isNotFound());
    }

    @Test
    void scanFurnitureOutsideOriginalBounds_isRepositionedWithoutRelaxingStrictValidation() throws Exception {
        mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scanFixture().replace("\"z\":1.015", "\"z\":0.90")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importWarnings[*].code", hasItem("FURNITURE_REPOSITIONED")));
    }

    @Test
    void invalidInfinityAndUnsupportedFurnitureType_remainRejected() throws Exception {
        mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"room\":{\"width\":1e309,\"depth\":3,\"height\":2},\"furniture\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"room":{"width":3,"depth":3,"height":2},"furniture":[
                                  {"id":"unknown-1","type":"not-supported","width":0.5,"depth":0.5,"height":0.5,
                                   "position":{"x":1,"z":1}}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_TYPE"));
    }

    @Test
    void furnitureThatCannotFitInAnyRotation_isExcludedButRoomUploadSucceeds() throws Exception {
        mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"room":{"width":3,"depth":3,"height":2},"furniture":[
                                  {"id":"too-large","type":"bed","width":4,"depth":4,"height":0.5,
                                   "position":{"x":1,"z":1}}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.furniture.length()").value(0))
                .andExpect(jsonPath("$.data.importWarnings[*].code", hasItem("FURNITURE_UNPLACED")));
    }

    @Test
    void headerlessScanUpload_staysInLegacyScope() throws Exception {
        String response = mockMvc.perform(post("/api/rooms/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scanFixture()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int roomId = ((Number) JsonPath.read(response, "$.data.roomId")).intValue();
        mockMvc.perform(get("/api/rooms/{roomId}", roomId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms/{roomId}", roomId).header("X-RoomFit-Client-Id", CLIENT_A))
                .andExpect(status().isNotFound());
    }

    private String scanFixture() {
        return """
                {
                  "name":"RoomPlan scan fixture without thumbnail",
                  "room":{"width":3.39,"depth":3.42,"height":2.4,"unit":"meter"},
                  "walls":[
                    {"id":"west","start":{"x":0,"z":0},"end":{"x":0,"z":3.42},"height":2.4,"thickness":0},
                    {"id":"east","start":{"x":3.36,"z":0},"end":{"x":3.36,"z":3.42},"height":2.4,"thickness":0},
                    {"id":"south","start":{"x":0,"z":0},"end":{"x":3.39,"z":0},"height":2.4,"thickness":0},
                    {"id":"north","start":{"x":0,"z":3.42},"end":{"x":3.39,"z":3.42},"height":2.4,"thickness":0}
                  ],
                  "openings":[],
                  "furniture":[
                    {"id":"bed-1","type":"bed","width":1.4,"depth":2.0,"height":0.45,"position":{"x":0.8,"z":1.015},"rotation":0},
                    {"id":"storage-1","type":"storage","width":0.8,"depth":0.4,"height":1.8,"position":{"x":2.0,"z":0.21},"rotation":0},
                    {"id":"chair-1","type":"chair","width":0.6,"depth":0.6,"height":0.8,"position":{"x":2.1,"z":0.32},"rotation":0},
                    {"id":"storage-2","type":"storage","width":0.8,"depth":0.4,"height":1.8,"position":{"x":2.7,"z":3.215},"rotation":0}
                  ]
                }
                """;
    }
}
