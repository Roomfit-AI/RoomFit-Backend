package com.roomfit.placement;

import com.jayway.jsonpath.JsonPath;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.agent.repository.AgentContextRepository;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import com.roomfit.room.RoomRepository;
import com.roomfit.room.RoomSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LayoutVariantIdLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private AgentContextRepository agentContextRepository;
    @Autowired
    private LayoutRepository layoutRepository;

    @Test
    void updateMoveFeedbackConfirmAndRoomRead_preserveAllBackendFurnitureFields() throws Exception {
        Layout layout = createLayout("desk-corner");

        String updateResponse = mockMvc.perform(put("/api/layouts/{layoutId}", layout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedFurniture[0].id").value("desk-variant-1"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].type").value("desk"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].label").value("책상"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].width").value(1.2))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].depth").value(0.6))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].height").value(0.73))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].status").value("USER_MODIFIED"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].productId").value("desk-product"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].variantId").value("desk-corner"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].styleTags[0]").value("minimal"))
                .andReturn().getResponse().getContentAsString();

        Integer updatedLayoutId = JsonPath.read(updateResponse, "$.data.layoutId");
        String feedbackResponse = mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "방이 넓어 보이게"
                                }
                                """.formatted(updatedLayoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendedFurniture[0].id").value("desk-variant-1"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].type").value("desk"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].label").value("책상"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].width").value(1.2))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].depth").value(0.6))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].height").value(0.73))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].status").value("USER_MODIFIED"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].productId").value("desk-product"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].variantId").value("desk-corner"))
                .andExpect(jsonPath("$.data.recommendedFurniture[0].styleTags[0]").value("minimal"))
                .andReturn().getResponse().getContentAsString();

        Integer feedbackLayoutId = JsonPath.read(feedbackResponse, "$.data.layoutId");
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", feedbackLayoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));

        mockMvc.perform(get("/api/rooms/{roomId}", layout.getRoomId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture[0].id").value("desk-variant-1"))
                .andExpect(jsonPath("$.data.furniture[0].type").value("desk"))
                .andExpect(jsonPath("$.data.furniture[0].label").value("책상"))
                .andExpect(jsonPath("$.data.furniture[0].width").value(1.2))
                .andExpect(jsonPath("$.data.furniture[0].depth").value(0.6))
                .andExpect(jsonPath("$.data.furniture[0].height").value(0.73))
                .andExpect(jsonPath("$.data.furniture[0].status").value("USER_MODIFIED"))
                .andExpect(jsonPath("$.data.furniture[0].productId").value("desk-product"))
                .andExpect(jsonPath("$.data.furniture[0].variantId").value("desk-corner"))
                .andExpect(jsonPath("$.data.furniture[0].styleTags[0]").value("minimal"));
    }

    @Test
    void legacyLayoutWithNullVariantId_remainsCompatible() throws Exception {
        Layout layout = createLayout(null);

        mockMvc.perform(put("/api/layouts/{layoutId}", layout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedFurniture[0].variantId").value(nullValue()));
    }

    private Layout createLayout(String variantId) {
        Room room = roomRepository.save(new Room(null, "Variant lifecycle room", 4.0, 4.0, 2.4,
                "meter", List.of(), List.of(), List.of(), RoomSource.ROOMPLAN, LocalDateTime.now(), null));
        AgentContext context = agentContextRepository.save(new AgentContext(room.getId(),
                LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL), List.of("desk"), List.of(),
                List.of(1L), List.of(), List.of("minimal")));
        Furniture furniture = new Furniture("desk-variant-1", "desk", "책상", 1.2, 0.6, 0.73,
                new Position(1.0, 1.0), 0, FurnitureStatus.RECOMMENDED,
                "desk-product", List.of("minimal"), variantId);
        return layoutRepository.save(new Layout(room.getId(), context.getId(),
                new ArrayList<>(List.of(furniture))));
    }

    private String updateBody() {
        return """
                {
                  "furniture": [
                    {
                      "id": "desk-variant-1",
                      "position": { "x": 1.1, "z": 1.1 },
                      "rotation": 0,
                      "status": "USER_MODIFIED",
                      "variantId": "desk-storage"
                    }
                  ]
                }
                """;
    }
}
