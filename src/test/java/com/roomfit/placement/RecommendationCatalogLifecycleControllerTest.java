package com.roomfit.placement;

import com.jayway.jsonpath.JsonPath;
import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.product.domain.MockProduct;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecommendationCatalogLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    static Stream<String> canonicalFurnitureTypes() {
        return GeneratedFurnitureCatalog.get().products().stream()
                .map(MockProduct::getType)
                .distinct();
    }

    @ParameterizedTest(name = "{0} persists, reloads, and confirms")
    @MethodSource("canonicalFurnitureTypes")
    void eachCanonicalType_survivesRecommendationLayoutAndRoomLifecycle(String furnitureType) throws Exception {
        String openings = "curtain_blind".equals(furnitureType)
                ? """
                  [{ "id": "window-1", "type": "window", "wall": "north",
                     "offset": 14, "width": 2, "height": 1, "sillHeight": 0.9 }]
                  """
                : "[]";
        long roomId = number(postJson("/api/rooms/upload", """
                { "name": "Catalog QA", "room": { "width": 30, "depth": 30, "height": 3 },
                  "openings": %s, "furniture": [] }
                """.formatted(openings)), "$.data.roomId");
        long contextId = number(postJson("/api/agent/context", """
                { "roomId": %d, "lifestyleGoal": "STUDY_FOCUSED", "designStyle": ["MINIMAL"],
                  "requiredItems": ["%s"], "optionalItems": [], "selectedImageIds": [1], "selectedProductIds": [] }
                """.formatted(roomId, furnitureType)), "$.data.contextId");

        String recommendation = postJson("/api/layouts/recommend", """
                { "roomId": %d, "contextId": %d }
                """.formatted(roomId, contextId));
        long layoutId = number(recommendation, "$.data.layoutId");

        mockMvc.perform(get("/api/layouts/{layoutId}", layoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedFurniture[0].type").value(furnitureType))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].productId", notNullValue()))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].variantId", notNullValue()));
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));
        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture[0].type").value(furnitureType))
                .andExpect(jsonPath("$.data.furniture[0].productId", notNullValue()))
                .andExpect(jsonPath("$.data.furniture[0].variantId", notNullValue()));
    }

    private String postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private long number(String body, String path) {
        return ((Number) JsonPath.read(body, path)).longValue();
    }
}
