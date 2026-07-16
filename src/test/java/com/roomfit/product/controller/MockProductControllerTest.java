package com.roomfit.product.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MockProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getMockProducts_returnsAllMvpFurnitureTypes() throws Exception {
        mockMvc.perform(get("/api/products/mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[*].type").value(containsInAnyOrder(
                        "bed", "desk", "chair", "storage", "rug", "lamp"
                )))
                .andExpect(jsonPath("$.data[*].productId").value(hasItems(
                        "desk-01", "chair-01", "lamp-01"
                )))
                .andExpect(jsonPath("$.data[*].productId", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].name", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].brand", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].width", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].depth", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].height", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].price", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].styleTags", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].imageUrl", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[0].purchaseUrl").value(nullValue()))
                .andExpect(jsonPath("$.data[1].purchaseUrl").value(
                        "https://www.ikea.com/kr/ko/p/micke-desk-white-80354281/"))
                .andExpect(jsonPath("$.data[*].requiredClearance.front", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].requiredClearance.side", everyItem(notNullValue())));
    }
}
