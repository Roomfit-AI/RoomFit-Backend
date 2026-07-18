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

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LayoutUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LayoutRepository layoutRepository;

    @Test
    void updateLayout_replacesFurnitureArrayAndReturnsValidationResult() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(put("/api/layouts/{layoutId}", layoutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 1.35, "z": 1.55 }, "rotation": 0, "status": "EXISTING" },
                                    { "id": "desk-1", "position": { "x": 3.0, "z": 1.05 }, "rotation": 0, "status": "EXISTING" },
                                    { "id": "chair-1", "position": { "x": 1.8, "z": 3.1 }, "rotation": 15, "status": "USER_MODIFIED" },
                                    { "id": "wardrobe-1", "position": { "x": 5.0, "z": 3.85 }, "rotation": 180, "status": "EXISTING" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.layoutId").value(layoutId))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.scoreSummary", notNullValue()))
                .andExpect(jsonPath("$.data.scoreSummary.collisionScore").value(100))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'chair-1')].position.x").value(hasItems(1.8)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'chair-1')].rotation").value(hasItems(15.0)))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.id == 'chair-1')].status").value(hasItems("USER_MODIFIED")))
                .andExpect(jsonPath("$.data.validationResult.collisionFree").value(true))
                .andExpect(jsonPath("$.data.validationResult.validationItems.length()").value(5));
    }

    @Test
    void updateLayout_withUnknownLayout_returnsLayoutNotFound() throws Exception {
        mockMvc.perform(put("/api/layouts/{layoutId}", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furniture": []
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("LAYOUT_NOT_FOUND"));
    }

    @Test
    void updateLayout_withConfirmedLayout_returnsAlreadyConfirmed() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutId))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/layouts/{layoutId}", layoutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("ALREADY_CONFIRMED"));
    }

    @Test
    void updateLayout_withMissingFurnitureArray_returnsInvalidRequestBody() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(put("/api/layouts/{layoutId}", layoutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void updateLayout_withEmptyFurnitureArray_returnsInvalidRequestBody() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(put("/api/layouts/{layoutId}", layoutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furniture": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void updateLayout_withInvalidFurnitureStatus_returnsInvalidFurnitureStatus() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(put("/api/layouts/{layoutId}", layoutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 1.35, "z": 1.55 }, "rotation": 0, "status": "EXISTING" },
                                    { "id": "desk-1", "position": { "x": 3.0, "z": 1.05 }, "rotation": 0, "status": "EXISTING" },
                                    { "id": "chair-1", "position": { "x": 3.0, "z": 1.85 }, "rotation": 180, "status": "MOVED" },
                                    { "id": "wardrobe-1", "position": { "x": 5.0, "z": 3.85 }, "rotation": 180, "status": "EXISTING" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_STATUS"));
    }

    @Test
    void updateLayout_withRotatedFootprintOutsideBoundary_returnsInvalidFurniturePosition() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(put("/api/layouts/{layoutId}", layoutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 1.35, "z": 1.55 }, "rotation": 0, "status": "EXISTING" },
                                    { "id": "desk-1", "position": { "x": 3.0, "z": 0.4 }, "rotation": 90, "status": "EXISTING" },
                                    { "id": "chair-1", "position": { "x": 1.8, "z": 3.1 }, "rotation": 15, "status": "USER_MODIFIED" },
                                    { "id": "wardrobe-1", "position": { "x": 5.0, "z": 3.85 }, "rotation": 180, "status": "EXISTING" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_POSITION"));
    }

    @Test
    void updateLayout_withNominallySafeButVisuallyOutsideVariant_returnsInvalidFurniturePosition() throws Exception {
        Long layoutId = createLayout();
        Layout layout = layoutRepository.findById(layoutId).orElseThrow();
        java.util.ArrayList<Furniture> furniture = new java.util.ArrayList<>(layout.getFurniture());
        Furniture current = furniture.getFirst();
        furniture.set(0, new Furniture(current.getId(), "plant", "코너 식물",
                0.6005779884792202, 0.6231243531863027, 0.8999999922374311,
                new Position(0.3802889942396101, 1.0), 0, FurnitureStatus.EXISTING,
                "plant-corner-01", java.util.List.of("natural"), "plant-corner"));
        layout.setFurniture(furniture);
        layoutRepository.save(layout);

        mockMvc.perform(put("/api/layouts/{layoutId}", layoutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 0.3802889942396101, "z": 1.0 }, "rotation": 0, "status": "EXISTING" },
                                    { "id": "desk-1", "position": { "x": 3.0, "z": 1.05 }, "rotation": 0, "status": "EXISTING" },
                                    { "id": "chair-1", "position": { "x": 3.0, "z": 1.85 }, "rotation": 180, "status": "EXISTING" },
                                    { "id": "wardrobe-1", "position": { "x": 5.0, "z": 3.85 }, "rotation": 180, "status": "EXISTING" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_POSITION"));
    }

    private Long createLayout() throws Exception {
        String contextResponse = mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["chair"],
                                  "optionalItems": [],
                                  "selectedImageIds": [1],
                                  "selectedProductIds": ["chair-01"]
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

    private String validUpdateRequest() {
        return """
                {
                  "furniture": [
                    { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0, "status": "EXISTING" },
                    { "id": "desk-1", "position": { "x": 2.7, "z": 0.4 }, "rotation": 90, "status": "EXISTING" },
                    { "id": "wardrobe-1", "position": { "x": 2.7, "z": 3.9 }, "rotation": 0, "status": "EXISTING" },
                    { "id": "chair-rec-1", "position": { "x": 1.8, "z": 3.1 }, "rotation": 15, "status": "USER_MODIFIED" }
                  ]
                }
                """;
    }
}
