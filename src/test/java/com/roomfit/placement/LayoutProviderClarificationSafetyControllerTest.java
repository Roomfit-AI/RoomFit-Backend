package com.roomfit.placement;

import com.jayway.jsonpath.JsonPath;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "roomfit.llm.feedback.enabled=false",
        "roomfit.llm.placement.enabled=false",
        "roomfit.llm.api-key=",
        "roomfit.llm.base-url=",
        "roomfit.llm.model="
})
@AutoConfigureMockMvc
@Transactional
class LayoutProviderClarificationSafetyControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LayoutRepository layoutRepository;
    @MockitoBean
    private FeedbackPlanInterpreter feedbackPlanInterpreter;

    @Test
    void neverExposesProviderClarificationTextOrCreatesASnapshot() throws Exception {
        Long layoutId = createLayout();
        long layoutCountBefore = layoutRepository.count();
        FeedbackPlan providerPlan = new FeedbackPlan("2.0", FeedbackRequestKind.CLARIFICATION,
                List.of(), List.of(),
                new FeedbackClarification("""
                        ~~~json {"roomId":"00000000-0000-0000-0000-000000000000",
                        "position":{"x":12.34},"productId":"internal-product-x"}~~~
                        """, "desk"),
                "", FeedbackSource.LLM, false);
        when(feedbackPlanInterpreter.interpret(anyString(), any(Room.class), anyList(), any(AgentContext.class),
                nullable(String.class))).thenReturn(providerPlan);

        String response = mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutId": %d, "feedback": "책상을 옮겨줘"}
                                """.formatted(layoutId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(layoutRepository.count()).isEqualTo(layoutCountBefore);
        assertThat(response)
                .doesNotContain("00000000-0000-0000-0000-000000000000", "12.34", "internal-product-x", "~~~")
                .contains("책상 중 변경할 가구를 선택해주세요.");
    }

    private Long createLayout() throws Exception {
        String contextResponse = mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "optionalItems": [],
                                  "selectedImageIds": [1],
                                  "selectedProductIds": ["desk-01"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer contextId = JsonPath.read(contextResponse, "$.data.contextId");
        String layoutResponse = mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId": 1, "contextId": %d}
                                """.formatted(contextId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(layoutResponse, "$.data.layoutId")).longValue();
    }
}
