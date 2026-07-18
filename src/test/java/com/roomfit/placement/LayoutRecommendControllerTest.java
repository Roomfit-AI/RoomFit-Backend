package com.roomfit.placement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class LayoutRecommendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LayoutRepository layoutRepository;

    @Test
    void recommend_returnsLayoutWithScoreSummaryAndValidationResult() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furnitureUpdates": [
                                    { "id": "bed-1", "status": "EXISTING" },
                                    { "id": "desk-1", "status": "DELETED" },
                                    { "id": "wardrobe-1", "status": "EXISTING" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        String contextResponse = mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL", "WHITE_TONE"],
                                  "requiredItems": ["bed", "desk", "chair"],
                                  "optionalItems": ["lamp"],
                                  "selectedImageIds": [1, 3],
                                  "selectedProductIds": ["desk-01", "chair-01", "lamp-01"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contextId = com.jayway.jsonpath.JsonPath.read(contextResponse, "$.data.contextId").toString();

        mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "contextId": %s
                                }
                                """.formatted(contextId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.layoutId", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.recommendedFurniture", notNullValue()))
                .andExpect(jsonPath("$.data.recommendedFurniture[*].status").value(hasItems("EXISTING", "RECOMMENDED")))
                .andExpect(jsonPath("$.data.recommendedFurniture[*].productId").value(hasItems("desk-01")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.productId == 'desk-01')].width").value(hasItems(1.2)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.productId == 'desk-01')].styleTags[0]").value(hasItems("minimal")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.type == 'bed' && @.status == 'RECOMMENDED')].id").isEmpty())
                .andExpect(jsonPath("$.data.scoreSummary.totalScore").value(590))
                .andExpect(jsonPath("$.data.scoreSummary.collisionScore").value(100))
                .andExpect(jsonPath("$.data.scoreSummary.boundaryScore").value(100))
                .andExpect(jsonPath("$.data.scoreSummary.doorWindowScore").value(100))
                .andExpect(jsonPath("$.data.scoreSummary.pathScore").value(100))
                .andExpect(jsonPath("$.data.scoreSummary.goalScore").value(95))
                .andExpect(jsonPath("$.data.scoreSummary.styleScore").value(95))
                .andExpect(jsonPath("$.data.validationResult.collisionFree").value(true))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true))
                .andExpect(jsonPath("$.data.validationResult.doorClearance").value(true))
                .andExpect(jsonPath("$.data.validationResult.windowClearance").value(true))
                .andExpect(jsonPath("$.data.validationResult.pathSecured").value(true))
                .andExpect(jsonPath("$.data.validationResult.validationItems.length()").value(5))
                .andExpect(jsonPath("$.data.validationResult.validationItems[*].type").value(hasItems(
                        "collision", "boundary", "door_clearance", "window_clearance", "path"
                )))
                .andExpect(jsonPath("$.data.validationResult.warnings", notNullValue()));
    }

    @Test
    void recommend_withUnknownContext_returnsContextNotFound() throws Exception {
        mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "contextId": 999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("CONTEXT_NOT_FOUND"));
    }

    @Test
    void recommend_withRoomThatDoesNotExist_returnsRoomNotFound() throws Exception {
        long contextId = createDeskContext(1);

        mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 999,
                                  "contextId": %d
                                }
                                """.formatted(contextId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void recommend_withContextFromAnotherRoom_returnsRoomContextMismatchWithoutCreatingLayout() throws Exception {
        long contextId = createDeskContext(1);
        long layoutsBefore = layoutRepository.count();

        mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 2,
                                  "contextId": %d
                                }
                                """.formatted(contextId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("ROOM_CONTEXT_MISMATCH"))
                .andExpect(jsonPath("$.error.message").value("요청한 방과 Agent Context의 방이 일치하지 않습니다."));

        assertThat(layoutRepository.count()).isEqualTo(layoutsBefore);
    }

    @Test
    void recommend_forCollectorStudio_returnsStudioRecommendationOnly() throws Exception {
        String contextResponse = mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 2,
                                  "lifestyleGoal": "RELAX_FOCUSED",
                                  "designStyle": ["MODERN"],
                                  "requiredItems": ["bed", "desk", "chair"],
                                  "optionalItems": ["lamp"],
                                  "selectedImageIds": [1, 3],
                                  "selectedProductIds": ["desk-01", "chair-01", "lamp-01"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contextId = com.jayway.jsonpath.JsonPath.read(contextResponse, "$.data.contextId").toString();

        mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 2,
                                  "contextId": %s
                                }
                                """.formatted(contextId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendedFurniture.length()").value(13))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'studio-bed')].status").value(hasItems("RECOMMENDED")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'studio-console')].status").value(hasItems("RECOMMENDED")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'studio-desk')].position.x").value(hasItems(2.8)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'studio-glass-shelf')].position.x").value(hasItems(4.45)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'studio-blue-cabinet')].position.x").value(hasItems(5.55)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'studio-rug')].position.x").value(hasItems(4.35)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'studio-cane-chair')].position.z").value(hasItems(org.hamcrest.Matchers.closeTo(5.2321, 0.001))))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'studio-cane-chair')].rotation").value(hasItems(225.0)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'bed-3')]").isEmpty());
    }

    private long createDeskContext(long roomId) throws Exception {
        String response = mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": %d,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "optionalItems": [],
                                  "selectedImageIds": [1],
                                  "selectedProductIds": []
                                }
                                """.formatted(roomId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.contextId")).longValue();
    }
}
