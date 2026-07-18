package com.roomfit.placement;

import com.jayway.jsonpath.JsonPath;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(properties = {
        "roomfit.llm.feedback.enabled=false",
        "roomfit.llm.api-key=",
        "roomfit.llm.base-url=",
        "roomfit.llm.model="
})
@AutoConfigureMockMvc
@Transactional
class LayoutFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LayoutRepository layoutRepository;

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
                .andExpect(jsonPath("$.data.feedbackStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.operationResults", hasSize(1)))
                .andExpect(jsonPath("$.data.operationResults[0].operationId").value("op-1"))
                .andExpect(jsonPath("$.data.operationResults[0].operationType").value("REPLACE_PRODUCT"))
                .andExpect(jsonPath("$.data.operationResults[0].status").value("APPLIED"))
                .andExpect(jsonPath("$.data.operationResults[0].resultFurnitureId", notNullValue()))
                .andExpect(jsonPath("$.data.operationResults[0].productId", notNullValue()))
                .andExpect(jsonPath("$.data.operationResults[0].variantId", notNullValue()))
                .andExpect(jsonPath("$.data.recommendedFurniture", notNullValue()))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.type == 'desk')]").value(hasSize(1)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.type == 'desk')].width")
                        .value(hasItem(greaterThanOrEqualTo(1.4))))
                .andExpect(jsonPath("$.data.scoreSummary.totalScore", notNullValue()))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true))
                .andExpect(jsonPath("$.data.validationResult.validationItems.length()").value(5))
                .andExpect(jsonPath("$.data.interpretedIntent.source").value("RULE_BASED"))
                .andExpect(jsonPath("$.data.interpretedIntent.version").value("2.0"))
                .andExpect(jsonPath("$.data.interpretedIntent.requestKind").value("DIRECT"))
                .andExpect(jsonPath("$.data.interpretedIntent.operations").value(hasItems("REPLACE_PRODUCT")))
                .andExpect(jsonPath("$.data.interpretedIntent.operationIds").value(hasItems("op-1")))
                .andExpect(jsonPath("$.data.interpretedIntent.rawIntent").value("LARGER_DESK"))
                .andExpect(jsonPath("$.data.interpretedIntent.targetFurniture").value("desk"))
                .andExpect(jsonPath("$.data.interpretedIntent.deskMinWidth").value(1.4))
                .andExpect(jsonPath("$.data.interpretedIntent.fallbackUsed").value(true))
                .andExpect(jsonPath("$.data.feedbackResult.applied").value(true))
                .andExpect(jsonPath("$.data.feedbackResult.operationsRequested").value(hasItems("REPLACE_PRODUCT")))
                .andExpect(jsonPath("$.data.feedbackResult.operationsApplied").value(hasItems("REPLACE_PRODUCT")));
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

    @Test
    void feedback_removeFurniturePersistsArrayRemovalThroughConfirmAndRoomRead() throws Exception {
        Long layoutId = createLayout();

        String feedbackResponse = mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "의자를 빼줘"
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.type == 'chair')]").value(hasSize(0)))
                .andExpect(jsonPath("$.data.feedbackResult.operationsApplied").value(hasItems("REMOVE_FURNITURE")))
                .andReturn().getResponse().getContentAsString();

        Integer feedbackLayoutId = JsonPath.read(feedbackResponse, "$.data.layoutId");
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", feedbackLayoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));

        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture[?(@.type == 'chair')]").value(hasSize(0)));
    }

    @Test
    void feedback_addFurnitureKeepsExistingResponseShapeAndUsesCatalogMetadata() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "구석에 조명을 하나 추가해줘"
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id =~ /mood-lamp-feedback-.*/)]")
                        .value(hasSize(1)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id =~ /mood-lamp-feedback-.*/)].type")
                        .value(hasItems("mood_lamp")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id =~ /mood-lamp-feedback-.*/)].productId")
                        .value(hasItems("lamp-floor-01")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id =~ /mood-lamp-feedback-.*/)].variantId")
                        .value(hasItems("lamp-floor")))
                .andExpect(jsonPath("$.data.interpretedIntent.operations").value(hasItems("ADD_FURNITURE")))
                .andExpect(jsonPath("$.data.feedbackResult.applied").value(true));
    }

    @Test
    void feedback_withAmbiguousTarget_returnsStructuredClarificationWithoutSnapshot() throws Exception {
        Long layoutId = createLayout();
        Layout layout = layoutRepository.findById(layoutId).orElseThrow();
        List<Furniture> furniture = new ArrayList<>(layout.getFurniture());
        furniture.add(new Furniture("desk-second", "desk", "보조 책상", 1.0, 0.6, 0.73,
                new Position(3.5, 3.5), 0, FurnitureStatus.RECOMMENDED));
        layout.setFurniture(furniture);
        layoutRepository.save(layout);

        mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "책상 더 크게"
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.layoutId").value(layoutId))
                .andExpect(jsonPath("$.data.feedbackStatus").value("NEEDS_CLARIFICATION"))
                .andExpect(jsonPath("$.data.operationResults", hasSize(0)))
                .andExpect(jsonPath("$.data.clarification.reasonCode").value("AMBIGUOUS_TARGET"))
                .andExpect(jsonPath("$.data.clarification.requiredField").value("targetFurnitureId"))
                .andExpect(jsonPath("$.data.clarification.candidates", hasSize(2)))
                .andExpect(jsonPath("$.data.clarification.candidates[0].furnitureId").value("desk-1"))
                .andExpect(jsonPath("$.data.clarification.candidates[1].furnitureId").value("desk-second"));
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
        removeNewRecommendedFurniture(layoutId.longValue());
        return layoutId.longValue();
    }

    private void removeNewRecommendedFurniture(Long layoutId) {
        Layout layout = layoutRepository.findById(layoutId).orElseThrow();
        layout.setFurniture(new java.util.ArrayList<>(layout.getFurniture().stream()
                .filter(item -> item.getStatus() != FurnitureStatus.RECOMMENDED)
                .toList()));
        layoutRepository.save(layout);
    }
}
