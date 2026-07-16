package com.roomfit.placement;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LayoutValidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validate_withWholeFurnitureArray_returnsValidationResult() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(layoutId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.collisionFree").value(true))
                .andExpect(jsonPath("$.data.boundaryValid").value(true))
                .andExpect(jsonPath("$.data.doorClearance").value(true))
                .andExpect(jsonPath("$.data.windowClearance").value(true))
                .andExpect(jsonPath("$.data.pathSecured").value(true))
                .andExpect(jsonPath("$.data.validationItems.length()").value(5))
                .andExpect(jsonPath("$.data.validationItems[*].type").value(hasItems(
                        "collision", "boundary", "door_clearance", "window_clearance", "path"
                )))
                .andExpect(jsonPath("$.data.warnings", notNullValue()));
    }

    @Test
    void validate_withCollision_returnsFailedCollisionItem() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0 },
                                    { "id": "desk-1", "position": { "x": 2.7, "z": 0.4 }, "rotation": 90 },
                                    { "id": "wardrobe-1", "position": { "x": 2.7, "z": 3.9 }, "rotation": 0 },
                                    { "id": "chair-rec-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0 }
                                  ]
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.collisionFree").value(false))
                .andExpect(jsonPath("$.data.validationItems[?(@.type == 'collision')].passed").value(hasItems(false)))
                .andExpect(jsonPath("$.data.warnings", notNullValue()));
    }

    @Test
    void validate_withUnknownFurnitureId_returnsFurnitureNotFound() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0 },
                                    { "id": "desk-1", "position": { "x": 2.7, "z": 0.4 }, "rotation": 90 },
                                    { "id": "wardrobe-1", "position": { "x": 2.7, "z": 3.9 }, "rotation": 0 },
                                    { "id": "chair-rec-1", "position": { "x": 1.6, "z": 3.1 }, "rotation": 0 },
                                    { "id": "ghost-1", "position": { "x": 1.0, "z": 1.0 }, "rotation": 0 }
                                  ]
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("FURNITURE_NOT_FOUND"));
    }

    @Test
    void validate_withMissingFurnitureId_returnsFurnitureArrayMismatch() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0 },
                                    { "id": "desk-1", "position": { "x": 2.7, "z": 0.4 }, "rotation": 90 },
                                    { "id": "wardrobe-1", "position": { "x": 2.7, "z": 3.9 }, "rotation": 0 }
                                  ]
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("FURNITURE_ARRAY_MISMATCH"));
    }

    @Test
    void validate_withMissingFurnitureArray_returnsInvalidRequestBody() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void validate_withEmptyFurnitureArray_returnsInvalidRequestBody() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "furniture": []
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void validate_withOutOfBoundaryPosition_returnsInvalidFurniturePosition() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0 },
                                    { "id": "desk-1", "position": { "x": 2.7, "z": 0.4 }, "rotation": 90 },
                                    { "id": "wardrobe-1", "position": { "x": 2.7, "z": 3.9 }, "rotation": 0 },
                                    { "id": "chair-rec-1", "position": { "x": 5.0, "z": 3.1 }, "rotation": 0 }
                                  ]
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_POSITION"));
    }

    @Test
    void validate_withInvalidRotation_returnsInvalidRotation() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "furniture": [
                                    { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0 },
                                    { "id": "desk-1", "position": { "x": 2.7, "z": 0.4 }, "rotation": 90 },
                                    { "id": "wardrobe-1", "position": { "x": 2.7, "z": 3.9 }, "rotation": 0 },
                                    { "id": "chair-rec-1", "position": { "x": 1.6, "z": 3.1 }, "rotation": 360 }
                                  ]
                                }
                                """.formatted(layoutId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_ROTATION"));
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

    private String validRequest(Long layoutId) {
        return """
                {
                  "layoutId": %d,
                  "furniture": [
                    { "id": "bed-1", "position": { "x": 0.8, "z": 1.4 }, "rotation": 0 },
                    { "id": "desk-1", "position": { "x": 2.7, "z": 0.5 }, "rotation": 90 },
                    { "id": "wardrobe-1", "position": { "x": 2.7, "z": 3.9 }, "rotation": 0 },
                    { "id": "chair-rec-1", "position": { "x": 1.6, "z": 3.1 }, "rotation": 0 }
                  ]
                }
                """.formatted(layoutId);
    }
}
