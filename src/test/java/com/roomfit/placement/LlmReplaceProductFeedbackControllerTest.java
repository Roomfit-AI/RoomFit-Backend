package com.roomfit.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.agent.repository.AgentContextRepository;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import com.roomfit.room.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(LlmReplaceProductFeedbackControllerTest.FakeLlmConfig.class)
class LlmReplaceProductFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AgentContextRepository agentContextRepository;

    @Autowired
    private LayoutRepository layoutRepository;

    @Test
    void fakeLlmStorageReplacementCreatesValidatedLayoutWithCatalogProduct() throws Exception {
        Room room = roomRepository.save(new Room(null, 6.0, 6.0, 2.4, "meter", List.of(), List.of()));
        AgentContext context = agentContextRepository.save(new AgentContext(room.getId(),
                LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L),
                List.of("desk-midcentury-glass-01"), List.of("midcentury", "modern")));
        Furniture desk = new Furniture("desk-rec-1", "desk", "미드센추리 글라스 책상",
                1.75, 0.74, 0.812, new Position(2.3, 1.0), 0,
                FurnitureStatus.RECOMMENDED, "desk-midcentury-glass-01",
                List.of("midcentury", "modern"), "desk-midcentury-glass");
        Layout baseLayout = layoutRepository.save(new Layout(room.getId(), context.getId(), List.of(desk)));

        mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "수납공간이 많은 책상으로 바꿔줘"
                                }
                                """.formatted(baseLayout.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.layoutId").value(not(baseLayout.getId().intValue())))
                .andExpect(jsonPath("$.data.interpretedIntent.source").value("LLM"))
                .andExpect(jsonPath("$.data.interpretedIntent.fallbackUsed").value(false))
                .andExpect(jsonPath("$.data.interpretedIntent.operations").value(hasItems("REPLACE_PRODUCT")))
                .andExpect(jsonPath("$.data.feedbackResult.applied").value(true))
                .andExpect(jsonPath("$.data.feedbackResult.source").value("LLM"))
                .andExpect(jsonPath("$.data.feedbackResult.fallbackUsed").value(false))
                .andExpect(jsonPath("$.data.feedbackResult.operationsRequested").value(hasItems("REPLACE_PRODUCT")))
                .andExpect(jsonPath("$.data.feedbackResult.operationsApplied").value(hasItems("REPLACE_PRODUCT")))
                .andExpect(jsonPath("$.data.feedbackResult.noChangeReason").doesNotExist())
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'desk-rec-1')].productId")
                        .value(hasItems("desk-storage-01")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'desk-rec-1')].variantId")
                        .value(hasItems("desk-storage")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'desk-rec-1')].width")
                        .value(hasItems(1.4)))
                .andExpect(jsonPath("$.data.validationResult.collisionFree").value(true))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true))
                .andExpect(jsonPath("$.data.validationResult.doorClearance").value(true))
                .andExpect(jsonPath("$.data.validationResult.windowClearance").value(true))
                .andExpect(jsonPath("$.data.validationResult.pathSecured").value(true));
    }

    @TestConfiguration
    static class FakeLlmConfig {

        @Bean
        @Primary
        FeedbackPlanInterpreter fakeLlmFeedbackPlanInterpreter(ObjectMapper objectMapper) {
            return new LlmFeedbackPlanInterpreter(prompt -> """
                    {
                      "version": "1.0",
                      "target": {"furnitureId": "desk-rec-1", "furnitureType": "desk"},
                      "operations": [
                        {"type": "REPLACE_PRODUCT", "constraints": {"storagePreferred": true}}
                      ],
                      "reason": "storage desk requested"
                    }
                    """, objectMapper);
        }
    }
}
