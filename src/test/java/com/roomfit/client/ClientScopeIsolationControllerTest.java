package com.roomfit.client;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ClientScopeIsolationControllerTest {

    private static final String CLIENT_A = "0c1c4a96-7f5b-48b7-9a01-111111111111";
    private static final String CLIENT_B = "0c1c4a96-7f5b-48b7-9a01-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clientScopedRoomsLayoutsDraftsAndFeedback_areIsolatedWithoutBreakingLegacyOrSamples() throws Exception {
        long roomA = uploadRoom("isolation-A", CLIENT_A);
        long roomB = uploadRoom("isolation-B", CLIENT_B);

        mockMvc.perform(get("/api/rooms/uploads/recent").header(ClientScopeService.HEADER_NAME, CLIENT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].roomId", hasItem((int) roomA)))
                .andExpect(jsonPath("$.data[*].roomId", not(hasItem((int) roomB))));
        mockMvc.perform(get("/api/rooms/uploads/recent").header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].roomId", hasItem((int) roomB)))
                .andExpect(jsonPath("$.data[*].roomId", not(hasItem((int) roomA))));

        mockMvc.perform(get("/api/rooms/{roomId}", roomA).header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", roomA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"furnitureUpdates\":[]}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/rooms/uploads/{roomId}", roomA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isNotFound());

        // Samples are public for reading, but scoped clients must work on a copy.
        mockMvc.perform(get("/api/rooms/1").header(ClientScopeService.HEADER_NAME, CLIENT_A))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms/1").header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/rooms/1/furniture")
                        .header(ClientScopeService.HEADER_NAME, CLIENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"furnitureUpdates\":[]}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/rooms/1/furniture")
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"furnitureUpdates\":[]}"))
                .andExpect(status().isNotFound());
        long sampleCopyA = copiedSample(CLIENT_A);
        long sampleCopyB = copiedSample(CLIENT_B);
        mockMvc.perform(get("/api/rooms/{roomId}", sampleCopyB).header(ClientScopeService.HEADER_NAME, CLIENT_A))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/rooms/{roomId}", sampleCopyA).header(ClientScopeService.HEADER_NAME, CLIENT_A))
                .andExpect(status().isOk());

        long contextA = createContext(roomA, CLIENT_A);
        long layoutA = recommend(roomA, contextA, CLIENT_A);

        mockMvc.perform(get("/api/layouts/{layoutId}", layoutA).header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/layouts/rooms/{roomId}/confirmed/latest", roomA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/layouts/feedback")
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"layoutId\":%d,\"feedback\":\"책상을 조금 더 넓게 쓰고 싶어\"}".formatted(layoutA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_A))
                .andExpect(status().isOk());
        long draftA = number(mockMvc.perform(post("/api/layouts/{layoutId}/draft", layoutA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_A))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.data.layoutId");

        mockMvc.perform(post("/api/layouts/{layoutId}/draft", layoutA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/layouts/{layoutId}", draftA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"furniture\":[]}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", draftA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", draftA)
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contextId\":%d}".formatted(contextA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/layouts/feedback")
                        .header(ClientScopeService.HEADER_NAME, CLIENT_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"layoutId\":%d,\"feedback\":\"책상을 조금 더 넓게 쓰고 싶어\"}".formatted(draftA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/layouts/feedback")
                        .header(ClientScopeService.HEADER_NAME, CLIENT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"layoutId\":%d,\"feedback\":\"책상을 조금 더 넓게 쓰고 싶어\"}".formatted(draftA)))
                .andExpect(status().isCreated());

        long legacyRoom = uploadRoom("isolation-legacy", null);
        mockMvc.perform(get("/api/rooms/uploads/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].roomId", hasItem((int) legacyRoom)));
        mockMvc.perform(get("/api/rooms/{roomId}", legacyRoom).header(ClientScopeService.HEADER_NAME, CLIENT_A))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientIdValidation_rejectsMalformedOrOverlongValues_andTreatsBlankAsLegacyWhenOptional() throws Exception {
        mockMvc.perform(get("/api/rooms/samples").header(ClientScopeService.HEADER_NAME, "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CLIENT_ID"));
        mockMvc.perform(get("/api/rooms/samples").header(ClientScopeService.HEADER_NAME, "x".repeat(65)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CLIENT_ID"));
        mockMvc.perform(get("/api/rooms/samples").header(ClientScopeService.HEADER_NAME, "   "))
                .andExpect(status().isOk());
    }

    private long uploadRoom(String name, String clientId) throws Exception {
        var request = post("/api/rooms/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "%s", "room": { "width": 4.5, "depth": 4.5, "height": 2.5, "unit": "meter" },
                          "openings": [], "furniture": [] }
                        """.formatted(name));
        if (clientId != null) {
            request.header(ClientScopeService.HEADER_NAME, clientId);
        }
        return number(mockMvc.perform(request).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(), "$.data.roomId");
    }

    private long copiedSample(String clientId) throws Exception {
        return number(mockMvc.perform(post("/api/rooms/1/copy").header(ClientScopeService.HEADER_NAME, clientId))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.data.roomId");
    }

    private long createContext(long roomId, String clientId) throws Exception {
        String response = mockMvc.perform(post("/api/agent/context")
                        .header(ClientScopeService.HEADER_NAME, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roomId": %d, "lifestyleGoal": "STUDY_FOCUSED", "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"], "optionalItems": [], "selectedImageIds": [1], "selectedProductIds": [] }
                                """.formatted(roomId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return number(response, "$.data.contextId");
    }

    private long recommend(long roomId, long contextId, String clientId) throws Exception {
        String response = mockMvc.perform(post("/api/layouts/recommend")
                        .header(ClientScopeService.HEADER_NAME, clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":%d,\"contextId\":%d}".formatted(roomId, contextId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return number(response, "$.data.layoutId");
    }

    private long number(String response, String path) {
        return ((Number) JsonPath.read(response, path)).longValue();
    }
}
