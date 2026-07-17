package com.roomfit.placement;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import com.roomfit.product.service.MockProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VariantProductFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MockProductService mockProductService;

    @BeforeEach
    void setUpProduct() {
        MockProduct product = new MockProduct("desk-product", "desk-compact", "desk", "컴팩트 책상",
                "RoomFit Mock", 1.2, 0.6, 0.73, 89000, List.of("minimal"),
                "/images/products/desk.png", null, new RequiredClearance(0.6, 0.2));
        when(mockProductService.findByProductIds(anyList())).thenReturn(List.of(product));
        when(mockProductService.findByProductId("desk-product")).thenReturn(product);
    }

    @Test
    void selectedProductVariantId_reachesAgentContextAndRecommendResponse() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/furniture", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "furnitureUpdates": [
                                    { "id": "desk-1", "status": "DELETED" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

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
                                  "selectedProductIds": ["desk-product"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.selectedProducts[0].productId").value("desk-product"))
                .andExpect(jsonPath("$.data.selectedProducts[0].variantId").value("desk-compact"))
                .andReturn().getResponse().getContentAsString();

        Integer contextId = com.jayway.jsonpath.JsonPath.read(contextResponse, "$.data.contextId");

        mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "contextId": %d }
                                """.formatted(contextId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendedFurniture[*].productId").value(hasItems("desk-product")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.productId == 'desk-product')].variantId")
                        .value(hasItems("desk-compact")));
    }
}
