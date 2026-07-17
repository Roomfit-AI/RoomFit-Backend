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
import com.roomfit.testsupport.DefaultTestGuest;
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
    @Autowired
    private DefaultTestGuest defaultTestGuest;

    @Test
    void updateFeedbackConfirmAndRoomRead_preserveVariantId() throws Exception {
        Layout layout = createLayout("desk-corner");

        String updateResponse = mockMvc.perform(put("/api/layouts/{layoutId}", layout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedFurniture[0].variantId").value("desk-corner"))
                .andReturn().getResponse().getContentAsString();

        Integer updatedLayoutId = JsonPath.read(updateResponse, "$.data.layoutId");
        String feedbackResponse = mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "책상을 조금 더 넓게 쓰고 싶어"
                                }
                                """.formatted(updatedLayoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendedFurniture[0].variantId").value("desk-corner"))
                .andReturn().getResponse().getContentAsString();

        Integer feedbackLayoutId = JsonPath.read(feedbackResponse, "$.data.layoutId");
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", feedbackLayoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));

        mockMvc.perform(get("/api/rooms/{roomId}", layout.getRoomId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture[*].variantId").value(hasItems("desk-corner")));
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
        // ownerId를 이 컨텍스트의 기본 테스트 게스트로 명시해야 한다 — 그렇지
        // 않으면 RoomAccessService.resolveAccessibleRoom이 ROOMPLAN 방인데
        // 소유자가 없다고 보고 404를 던진다(다른 게스트 소유 취급).
        Room room = roomRepository.save(new Room(null, "Variant lifecycle room", 4.0, 4.0, 2.4,
                "meter", List.of(), List.of(), List.of(), RoomSource.ROOMPLAN, LocalDateTime.now(), null,
                defaultTestGuest.guestId(), null));
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
