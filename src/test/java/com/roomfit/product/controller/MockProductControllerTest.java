package com.roomfit.product.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
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
        String response = mockMvc.perform(get("/api/products/mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data[*].productId").value(hasItems(
                        "bed-01", "desk-01", "chair-01", "storage-01", "rug-01", "lamp-01",
                        "desk-compact-01", "desk-storage-01", "desk-corner-01", "desk-midcentury-glass-01"
                )))
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> products = JsonPath.read(response, "$.data");
        Set<String> types = products.stream().map(product -> (String) product.get("type")).collect(Collectors.toSet());
        assertThat(types).contains("bed", "desk", "chair", "storage", "rug", "lamp");
        assertThat(products).extracting(product -> product.get("productId")).doesNotHaveDuplicates();

        Map<String, Object> legacyDesk = productById(products, "desk-01");
        assertThat(legacyDesk.get("variantId")).isNull();
        assertThat(legacyDesk.get("brand")).isEqualTo("RoomFit Mock");
        assertThat(legacyDesk.get("price")).isEqualTo(89000);
        assertThat(legacyDesk.get("imageUrl")).isEqualTo("/images/products/desk-white.png");

        Map<String, Object> compactDesk = productById(products, "desk-compact-01");
        assertThat(compactDesk.get("variantId")).isEqualTo("desk-compact");
        assertThat(compactDesk).containsKeys("brand", "price", "imageUrl");
        assertThat(compactDesk.get("brand")).isNull();
        assertThat(compactDesk.get("price")).isNull();
        assertThat(compactDesk.get("imageUrl")).isNull();
        assertThat(compactDesk.get("purchaseUrl"))
                .isEqualTo("https://www.ikea.com/kr/ko/p/lagkapten-adils-desk-white-s09416759/");
    }

    private Map<String, Object> productById(List<Map<String, Object>> products, String productId) {
        return products.stream()
                .filter(product -> productId.equals(product.get("productId")))
                .findFirst()
                .orElseThrow();
    }
}
