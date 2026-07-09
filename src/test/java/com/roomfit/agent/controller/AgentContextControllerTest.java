package com.roomfit.agent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentContextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createContext_returnsContextWithStyleTagsAndSelectedProducts() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL", "WHITE_TONE"],
                                  "requiredItems": ["bed", "desk", "chair"],
                                  "optionalItems": ["storage", "rug", "lamp"],
                                  "selectedImageIds": [1, 3],
                                  "selectedProductIds": ["desk-01", "chair-01"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.contextId", notNullValue()))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.lifestyleGoal").value("STUDY_FOCUSED"))
                .andExpect(jsonPath("$.data.designStyle").value(hasItems("MINIMAL", "WHITE_TONE")))
                .andExpect(jsonPath("$.data.requiredItems").value(hasItems("bed", "desk", "chair")))
                .andExpect(jsonPath("$.data.optionalItems").value(hasItems("storage", "rug", "lamp")))
                .andExpect(jsonPath("$.data.selectedImageIds").value(hasItems(1, 3)))
                .andExpect(jsonPath("$.data.selectedProductIds").value(hasItems("desk-01", "chair-01")))
                .andExpect(jsonPath("$.data.styleTags").value(hasItems(
                        "minimal", "white_tone", "open_space", "study", "desk_zone"
                )))
                .andExpect(jsonPath("$.data.selectedProducts.length()").value(2))
                .andExpect(jsonPath("$.data.selectedProducts[0].productId").value("desk-01"))
                .andExpect(jsonPath("$.data.selectedProducts[0].width").value(1.2))
                .andExpect(jsonPath("$.data.selectedProducts[0].requiredClearance.front").value(0.6))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()));
    }

    @Test
    void createContext_withoutSelectedProducts_returnsEmptySelectedProducts() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "RELAX_FOCUSED",
                                  "designStyle": ["COZY"],
                                  "requiredItems": ["bed"],
                                  "selectedImageIds": [2]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.selectedProductIds.length()").value(0))
                .andExpect(jsonPath("$.data.selectedProducts.length()").value(0))
                .andExpect(jsonPath("$.data.optionalItems.length()").value(0))
                .andExpect(jsonPath("$.data.styleTags").value(hasItems("natural", "wood_tone", "cozy")));
    }

    @Test
    void createContext_withUnknownRoom_returnsRoomNotFound() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 999,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "selectedImageIds": [1]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void createContext_withEmptyRequiredItems_returnsRequiredItemEmpty() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": [],
                                  "selectedImageIds": [1]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUIRED_ITEM_EMPTY"));
    }

    @Test
    void createContext_withEmptySelectedImageIds_returnsStyleImageEmpty() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "selectedImageIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("STYLE_IMAGE_EMPTY"));
    }

    @Test
    void createContext_withUnknownStyleImage_returnsStyleImageNotFound() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "selectedImageIds": [999]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STYLE_IMAGE_NOT_FOUND"));
    }

    @Test
    void createContext_withUnknownProduct_returnsProductNotFound() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "selectedImageIds": [1],
                                  "selectedProductIds": ["missing-product"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void createContext_withInvalidLifestyleGoal_returnsInvalidLifestyleGoal() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "INVALID",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["desk"],
                                  "selectedImageIds": [1]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_LIFESTYLE_GOAL"));
    }

    @Test
    void createContext_withInvalidDesignStyle_returnsInvalidDesignStyle() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["INVALID"],
                                  "requiredItems": ["desk"],
                                  "selectedImageIds": [1]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_DESIGN_STYLE"));
    }

    @Test
    void createContext_withInvalidFurnitureType_returnsInvalidFurnitureType() throws Exception {
        mockMvc.perform(post("/api/agent/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "lifestyleGoal": "STUDY_FOCUSED",
                                  "designStyle": ["MINIMAL"],
                                  "requiredItems": ["sofa"],
                                  "selectedImageIds": [1]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FURNITURE_TYPE"));
    }
}
