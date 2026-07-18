package com.roomfit.placement;

import com.jayway.jsonpath.JsonPath;
import com.roomfit.room.Furniture;
import com.roomfit.room.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LayoutConfirmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LayoutRepository layoutRepository;

    @Test
    void confirmLayout_returnsConfirmedLayout() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.layoutId").value(layoutId))
                .andExpect(jsonPath("$.data.confirmed").value(true))
                .andExpect(jsonPath("$.data.confirmedAt", notNullValue()));
    }

    @Test
    void confirmLayout_withUnknownLayout_returnsLayoutNotFound() throws Exception {
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("LAYOUT_NOT_FOUND"));
    }

    @Test
    void confirmLayout_withAlreadyConfirmedLayout_returnsAlreadyConfirmed() throws Exception {
        Long layoutId = createLayout();

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("ALREADY_CONFIRMED"));
    }

    @Test
    void confirmLayout_withBoundaryInvalidSnapshot_rejectsBeforeConfirming() throws Exception {
        Long layoutId = createLayout();
        Layout layout = layoutRepository.findById(layoutId).orElseThrow();
        ArrayList<Furniture> furniture = new ArrayList<>(layout.getFurniture());
        Furniture current = furniture.getFirst();
        furniture.set(0, new Furniture(current.getId(), current.getType(), current.getLabel(),
                current.getWidth(), current.getDepth(), current.getHeight(), new Position(0.01, 0.01),
                45, current.getStatus(), current.getProductId(), current.getStyleTags(), current.getVariantId()));
        layout.setFurniture(furniture);
        layoutRepository.save(layout);

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_POSITION"));

        org.assertj.core.api.Assertions.assertThat(layoutRepository.findById(layoutId).orElseThrow().isConfirmed())
                .isFalse();
    }

    @Test
    void confirmLayout_rejectsVariantWhoseVisualBoundsExceedNominalBoundary() throws Exception {
        Long layoutId = createLayout();
        Layout layout = layoutRepository.findById(layoutId).orElseThrow();
        ArrayList<Furniture> furniture = new ArrayList<>(layout.getFurniture());
        Furniture current = furniture.getFirst();
        furniture.set(0, new Furniture(current.getId(), "plant", "코너 식물",
                0.6005779884792202, 0.6231243531863027, 0.8999999922374311,
                new Position(0.3802889942396101, 1.0), 0, current.getStatus(),
                "plant-corner-01", java.util.List.of("natural"), "plant-corner"));
        layout.setFurniture(furniture);
        layoutRepository.save(layout);

        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", layoutId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_POSITION"));

        org.assertj.core.api.Assertions.assertThat(layoutRepository.findById(layoutId).orElseThrow().isConfirmed())
                .isFalse();
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
        return layoutId.longValue();
    }
}
