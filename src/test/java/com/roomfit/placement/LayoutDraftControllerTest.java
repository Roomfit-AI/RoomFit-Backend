package com.roomfit.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.room.Furniture;
import com.roomfit.room.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LayoutDraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LayoutRepository layoutRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createDraft_deepCopiesConfirmedLayoutWithoutMutatingSource() throws Exception {
        Long sourceId = createConfirmedLayout();
        Layout sourceBefore = layoutRepository.findById(sourceId).orElseThrow();
        Furniture sourceDesk = sourceBefore.getFurniture().stream()
                .filter(item -> "desk".equals(item.getType()))
                .findFirst()
                .orElseThrow();

        String response = mockMvc.perform(post("/api/layouts/{layoutId}/draft", sourceId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.layoutId", notNullValue()))
                .andExpect(jsonPath("$.data.sourceLayoutId").value(sourceId))
                .andExpect(jsonPath("$.data.roomId").value(sourceBefore.getRoomId()))
                .andExpect(jsonPath("$.data.confirmed").value(false))
                .andExpect(jsonPath("$.data.confirmedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.recommendedFurniture", hasSize(sourceBefore.getFurniture().size())))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true))
                .andReturn().getResponse().getContentAsString();

        Long draftId = ((Number) JsonPath.read(response, "$.data.layoutId")).longValue();
        Layout draft = layoutRepository.findById(draftId).orElseThrow();
        Furniture draftDesk = draft.getFurniture().stream()
                .filter(item -> sourceDesk.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(draftId).isNotEqualTo(sourceId);
        assertThat(draft.isConfirmed()).isFalse();
        assertThat(draft.getConfirmedAt()).isNull();
        assertThat(draft.getSourceLayoutId()).isEqualTo(sourceId);
        assertThat(draftDesk).isNotSameAs(sourceDesk);
        assertThat(draftDesk.getPosition()).isNotSameAs(sourceDesk.getPosition());
        assertThat(draftDesk.getPosition().getX()).isEqualTo(sourceDesk.getPosition().getX());
        assertThat(draftDesk.getPosition().getZ()).isEqualTo(sourceDesk.getPosition().getZ());
        assertThat(draftDesk.getRotation()).isEqualTo(sourceDesk.getRotation());
        assertThat(draftDesk.getProductId()).isEqualTo(sourceDesk.getProductId());
        assertThat(draftDesk.getVariantId()).isEqualTo(sourceDesk.getVariantId());
        assertThat(draftDesk.getProductId()).isEqualTo("desk-compact-01");
        assertThat(draftDesk.getVariantId()).isEqualTo("desk-compact");
        assertThat(draftDesk.getStyleTags()).containsExactlyElementsOf(sourceDesk.getStyleTags());
        assertThat(draftDesk.getStatus()).isEqualTo(sourceDesk.getStatus());

        Layout sourceAfter = layoutRepository.findById(sourceId).orElseThrow();
        assertThat(sourceAfter.isConfirmed()).isTrue();
        assertThat(sourceAfter.getConfirmedAt()).isNotNull();
        assertThat(sourceAfter.getFurniture()).usingRecursiveComparison()
                .isEqualTo(sourceBefore.getFurniture());
    }

    @Test
    void confirmedToDraft_updateFeedbackConfirm_preservesSourceHistory() throws Exception {
        Long sourceId = createConfirmedLayout();
        Long draftId = createDraft(sourceId);
        Layout draft = layoutRepository.findById(draftId).orElseThrow();

        String updateResponse = mockMvc.perform(put("/api/layouts/{layoutId}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload(draft, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layoutId").value(draftId))
                .andExpect(jsonPath("$.data.confirmed").value(false))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true))
                .andReturn().getResponse().getContentAsString();
        Number movedX = JsonPath.read(updateResponse, "$.data.recommendedFurniture[0].position.x");

        mockMvc.perform(put("/api/layouts/{layoutId}", sourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload(layoutRepository.findById(sourceId).orElseThrow(), false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_CONFIRMED"));

        String feedbackResponse = mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "책상 더 크게"
                                }
                                """.formatted(draftId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.sourceLayoutId").value(draftId))
                .andExpect(jsonPath("$.data.confirmed").value(false))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true))
                .andReturn().getResponse().getContentAsString();
        Long feedbackLayoutId = ((Number) JsonPath.read(feedbackResponse, "$.data.layoutId")).longValue();

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", feedbackLayoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));

        mockMvc.perform(get("/api/rooms/{roomId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture[0].position.x").value(movedX.doubleValue()))
                .andExpect(jsonPath("$.data.furniture", hasSize(draft.getFurniture().size())));

        assertThat(layoutRepository.findById(sourceId).orElseThrow().isConfirmed()).isTrue();
        assertThat(layoutRepository.findById(draftId).orElseThrow().isConfirmed()).isFalse();
        assertThat(layoutRepository.findById(feedbackLayoutId).orElseThrow().isConfirmed()).isTrue();
    }

    @Test
    void latestConfirmedAndDraftRead_returnLifecycleState() throws Exception {
        Long sourceId = createConfirmedLayout();

        mockMvc.perform(get("/api/layouts/rooms/{roomId}/confirmed/latest", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layoutId").value(sourceId))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.confirmed").value(true));

        Long draftId = createDraft(sourceId);
        mockMvc.perform(get("/api/layouts/{layoutId}", draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layoutId").value(draftId))
                .andExpect(jsonPath("$.data.sourceLayoutId").value(sourceId))
                .andExpect(jsonPath("$.data.confirmed").value(false));
    }

    @Test
    void createDraft_rejectsUnconfirmedOrBoundaryInvalidSource() throws Exception {
        Long unconfirmedId = createLayout();
        mockMvc.perform(post("/api/layouts/{layoutId}/draft", unconfirmedId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("LAYOUT_NOT_CONFIRMED"));

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", unconfirmedId))
                .andExpect(status().isOk());
        Layout invalid = layoutRepository.findById(unconfirmedId).orElseThrow();
        List<Furniture> furniture = new ArrayList<>(invalid.getFurniture());
        Furniture first = furniture.getFirst();
        furniture.set(0, new Furniture(first.getId(), first.getType(), first.getLabel(),
                first.getWidth(), first.getDepth(), first.getHeight(), new Position(0.01, 0.01),
                first.getRotation(), first.getStatus(), first.getProductId(), first.getStyleTags(), first.getVariantId()));
        invalid.setFurniture(furniture);
        layoutRepository.save(invalid);

        mockMvc.perform(post("/api/layouts/{layoutId}/draft", unconfirmedId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_POSITION"));
    }

    @Test
    void reeditDraft_preservesManagedFurnitureAcrossAddFeedbackConfirmAndRoomRead() throws Exception {
        Long sourceId = createConfirmedLayout();
        Layout sourceBefore = layoutRepository.findById(sourceId).orElseThrow();
        Long draftId = createDraft(sourceId);
        Layout draft = layoutRepository.findById(draftId).orElseThrow();

        mockMvc.perform(put("/api/layouts/{layoutId}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reeditPayload(draft)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layoutId").value(draftId))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true));

        Layout managedDraft = layoutRepository.findById(draftId).orElseThrow();
        int managedFurnitureCount = managedDraft.getFurniture().size();
        Map<String, Furniture> managedById = managedDraft.getFurniture().stream()
                .collect(Collectors.toMap(Furniture::getId, Function.identity()));
        String movedFurnitureId = managedDraft.getFurniture().getFirst().getId();
        double movedFurnitureX = managedDraft.getFurniture().getFirst().getPosition().getX();
        String deletedFurnitureId = managedDraft.getFurniture().getLast().getId();
        Long contextId = createAgentContext(List.of("desk", "lamp"));

        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "contextId": %d }
                                """.formatted(contextId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layoutId").value(draftId))
                .andExpect(jsonPath("$.data.sourceLayoutId").value(sourceId))
                .andExpect(jsonPath("$.data.confirmed").value(false))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true));

        Layout withAddition = layoutRepository.findById(draftId).orElseThrow();
        assertThat(withAddition.getFurniture()).hasSize(managedFurnitureCount + 2);
        assertThat(withAddition.getFurniture().stream().filter(item -> "desk".equals(item.getType())
                && !managedById.containsKey(item.getId()))).singleElement()
                .satisfies(item -> assertThat(item.getStatus()).isEqualTo(com.roomfit.room.FurnitureStatus.RECOMMENDED));
        assertThat(withAddition.getFurniture().stream().filter(item -> "mood_lamp".equals(item.getType())))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getProductId()).isNotBlank();
                    assertThat(item.getStatus().name()).isEqualTo("RECOMMENDED");
                });
        for (Map.Entry<String, Furniture> entry : managedById.entrySet()) {
            Furniture after = withAddition.getFurniture().stream()
                    .filter(item -> entry.getKey().equals(item.getId()))
                    .findFirst()
                    .orElseThrow();
            Furniture before = entry.getValue();
            assertThat(after.getPosition().getX()).isEqualTo(before.getPosition().getX());
            assertThat(after.getPosition().getZ()).isEqualTo(before.getPosition().getZ());
            assertThat(after.getRotation()).isEqualTo(before.getRotation());
            assertThat(after.getStatus()).isEqualTo(before.getStatus());
            assertThat(after.getProductId()).isEqualTo(before.getProductId());
            assertThat(after.getVariantId()).isEqualTo(before.getVariantId());
            assertThat(after.getStyleTags()).containsExactlyElementsOf(before.getStyleTags());
        }

        mockMvc.perform(get("/api/layouts/{layoutId}", draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layoutId").value(draftId))
                .andExpect(jsonPath("$.data.confirmed").value(false));

        String feedbackResponse = mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "layoutId": %d, "feedback": "책상 더 크게" }
                                """.formatted(draftId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sourceLayoutId").value(sourceId))
                .andReturn().getResponse().getContentAsString();
        Long feedbackLayoutId = ((Number) JsonPath.read(feedbackResponse, "$.data.layoutId")).longValue();

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", feedbackLayoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));

        mockMvc.perform(get("/api/rooms/{roomId}", sourceBefore.getRoomId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.furniture[?(@.id == '%s')].position.x"
                        .formatted(movedFurnitureId)).value(movedFurnitureX))
                .andExpect(jsonPath("$.data.furniture[?(@.id == '%s')].status"
                        .formatted(deletedFurnitureId)).value("DELETED"))
                .andExpect(jsonPath("$.data.furniture[?(@.type == 'mood_lamp')]").isNotEmpty());

        assertThat(layoutRepository.findById(sourceId).orElseThrow().getFurniture())
                .usingRecursiveComparison()
                .isEqualTo(sourceBefore.getFurniture());
    }

    @Test
    void addFurniture_keepsExistingCollisionAndRequiresTheNewFurnitureToBeSafe() throws Exception {
        Long sourceId = createConfirmedLayout();
        Long draftId = createDraft(sourceId);
        Layout draft = layoutRepository.findById(draftId).orElseThrow();
        int draftFurnitureCount = draft.getFurniture().size();
        List<String> draftFurnitureIds = draft.getFurniture().stream().map(Furniture::getId).toList();
        Furniture collisionTarget = draft.getFurniture().get(1);
        Position expectedCollisionPosition = new Position(
                collisionTarget.getPosition().getX(), collisionTarget.getPosition().getZ() + 0.45);

        List<Map<String, Object>> furniture = new ArrayList<>();
        for (int index = 0; index < draft.getFurniture().size(); index++) {
            Furniture item = draft.getFurniture().get(index);
            Position position = index == 0 ? expectedCollisionPosition : item.getPosition();
            furniture.add(Map.of(
                    "id", item.getId(),
                    "position", Map.of("x", position.getX(), "z", position.getZ()),
                    "rotation", item.getRotation(),
                    "status", index == 0 ? "USER_MODIFIED" : item.getStatus().name()
            ));
        }

        mockMvc.perform(put("/api/layouts/{layoutId}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("furniture", furniture))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validationResult.collisionFree").value(false))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true));

        Long contextId = createAgentContext(List.of("lamp"));
        mockMvc.perform(post("/api/layouts/{layoutId}/furniture-additions", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "contextId": %d }
                                """.formatted(contextId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.layoutId").value(draftId))
                .andExpect(jsonPath("$.data.validationResult.collisionFree").value(false))
                .andExpect(jsonPath("$.data.validationResult.boundaryValid").value(true))
                .andExpect(jsonPath("$.data.recommendedFurniture", hasSize(draftFurnitureCount + 1)));

        Layout updated = layoutRepository.findById(draftId).orElseThrow();
        assertThat(updated.getFurniture()).hasSize(draftFurnitureCount + 1);
        Furniture added = updated.getFurniture().stream()
                .filter(item -> !draftFurnitureIds.contains(item.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(GeneratedFurnitureCatalog.get().normalizeType(added.getType())).isEqualTo("mood_lamp");
        assertThat(updated.getFurniture().getFirst().getPosition().getX())
                .isEqualTo(expectedCollisionPosition.getX());
        assertThat(updated.getFurniture().getFirst().getPosition().getZ())
                .isEqualTo(expectedCollisionPosition.getZ());
    }

    private Long createConfirmedLayout() throws Exception {
        Long layoutId = createLayout();
        Layout layout = layoutRepository.findById(layoutId).orElseThrow();
        List<Furniture> furniture = new ArrayList<>(layout.getFurniture().stream()
                .map(item -> "desk".equals(item.getType())
                        ? new Furniture(item.getId(), item.getType(), item.getLabel(),
                        item.getWidth(), item.getDepth(), item.getHeight(),
                        new Position(item.getPosition().getX(), item.getPosition().getZ()),
                        item.getRotation(), item.getStatus(), "desk-compact-01",
                        List.of("minimal", "classic"), "desk-compact")
                        : item)
                .toList());
        layout.setFurniture(furniture);
        layoutRepository.save(layout);
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutId))
                .andExpect(status().isOk());
        return layoutId;
    }

    private Long createDraft(Long sourceId) throws Exception {
        String response = mockMvc.perform(post("/api/layouts/{layoutId}/draft", sourceId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.layoutId")).longValue();
    }

    private Long createLayout() throws Exception {
        Long contextId = createAgentContext(List.of("chair"));

        String layoutResponse = mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "contextId": %d
                                }
                                """.formatted(contextId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(layoutResponse, "$.data.layoutId")).longValue();
    }

    private Long createAgentContext(List<String> requiredItems) throws Exception {
        String contextResponse = mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": %s,
                                  "optionalItems": [],
                                  "selectedImageIds": [1],
                                  "selectedProductIds": ["desk-compact-01"]
                                }
                                """.formatted(objectMapper.writeValueAsString(requiredItems))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(contextResponse, "$.data.contextId")).longValue();
    }

    private String updatePayload(Layout layout, boolean moveFirst) throws Exception {
        List<Map<String, Object>> furniture = new ArrayList<>();
        for (int index = 0; index < layout.getFurniture().size(); index++) {
            Furniture item = layout.getFurniture().get(index);
            double x = item.getPosition().getX();
            if (moveFirst && index == 0) {
                x += x < 2.9 ? 0.05 : -0.05;
            }
            Map<String, Object> update = new LinkedHashMap<>();
            update.put("id", item.getId());
            update.put("position", Map.of("x", x, "z", item.getPosition().getZ()));
            boolean rotate = moveFirst && "chair".equals(item.getType());
            update.put("rotation", rotate ? (item.getRotation() + 15) % 360 : item.getRotation());
            update.put("status", moveFirst && (index == 0 || rotate) ? "USER_MODIFIED" : item.getStatus().name());
            furniture.add(update);
        }
        return objectMapper.writeValueAsString(Map.of("furniture", furniture));
    }

    private String reeditPayload(Layout layout) throws Exception {
        List<Map<String, Object>> furniture = new ArrayList<>();
        for (int index = 0; index < layout.getFurniture().size(); index++) {
            Furniture item = layout.getFurniture().get(index);
            Map<String, Object> update = new LinkedHashMap<>();
            update.put("id", item.getId());
            update.put("position", Map.of(
                    "x", index == 0 ? item.getPosition().getX() + 0.05 : item.getPosition().getX(),
                    "z", item.getPosition().getZ()
            ));
            update.put("rotation", "chair".equals(item.getType())
                    ? (item.getRotation() + 90) % 360
                    : item.getRotation());
            update.put("status", index == layout.getFurniture().size() - 1
                    ? "DELETED"
                    : index == 0 || "chair".equals(item.getType()) ? "USER_MODIFIED" : item.getStatus().name());
            furniture.add(update);
        }
        return objectMapper.writeValueAsString(Map.of("furniture", furniture));
    }
}
