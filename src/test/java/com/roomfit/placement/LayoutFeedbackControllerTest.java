package com.roomfit.placement;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LayoutFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void feedback_withLargerDesk_returnsReRecommendedLayout() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "책상을 조금 더 넓게 쓰고 싶어"
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.layoutId", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.recommendedFurniture", notNullValue()))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.type == 'desk')]").value(hasSize(1)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.type == 'desk')].width")
                        .value(hasItem(greaterThanOrEqualTo(1.4))))
                .andExpect(jsonPath("$.data.scoreSummary.totalScore", notNullValue()))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true))
                .andExpect(jsonPath("$.data.validationResult.validationItems.length()").value(5))
                .andExpect(jsonPath("$.data.interpretedIntent.source").value("RULE_BASED"))
                .andExpect(jsonPath("$.data.interpretedIntent.rawIntent").value("LARGER_DESK"))
                .andExpect(jsonPath("$.data.interpretedIntent.targetFurniture").value("desk"))
                .andExpect(jsonPath("$.data.interpretedIntent.deskMinWidth").value(1.4))
                .andExpect(jsonPath("$.data.interpretedIntent.fallbackUsed").value(true));
    }

    @Test
    void feedback_withStoragePriority_returnsInterpretedIntent() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "수납 늘려줘"
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.type == 'desk')].productId")
                        .value(hasItems("desk-storage-01")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.type == 'desk')].variantId")
                        .value(hasItems("desk-storage")))
                .andExpect(jsonPath("$.data.interpretedIntent.storagePriority").value("HIGH"));
    }

    @Test
    void feedback_withOpenSpacePriority_returnsInterpretedIntent() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "방이 넓어 보이게"
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.interpretedIntent.openSpacePriority").value("HIGH"));
    }

    @Test
    void feedback_withUnsupportedSentence_returnsUnsupportedFeedbackIntent() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "침대 색 바꿔줘"
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FEEDBACK_INTENT"));
    }

    @Test
    void feedback_withUnknownLayout_returnsLayoutNotFound() throws Exception {
        mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": 99999,
                                  "feedback": "책상 더 크게"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("LAYOUT_NOT_FOUND"));
    }

    private Long createLayout() throws Exception {
        String contextResponse = mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk", "chair"],
                                  "optionalItems": [],
                                  "selectedImageIds": [1],
                                  "selectedProductIds": ["desk-01", "chair-01"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer contextId = JsonPath.read(contextResponse, "$.data.contextId");

        String layoutResponse = mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "contextId": %d
                                }
                                """.formatted(contextId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer layoutId = JsonPath.read(layoutResponse, "$.data.layoutId");
        return layoutId.longValue();
    }
}
