package com.roomfit.style.controller;

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
class StyleImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getStyleImages_returnsFixedStyleImageContract() throws Exception {
        mockMvc.perform(get("/api/styles/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[*].imageId").value(containsInAnyOrder(1, 2, 3, 4, 5)))
                .andExpect(jsonPath("$.data[*].title").value(hasItems(
                        "미니멀 원룸 인테리어",
                        "내추럴 원룸 인테리어",
                        "모던 원룸 인테리어",
                        "클래식 원룸 인테리어",
                        "미드센추리 원룸 인테리어"
                )))
                .andExpect(jsonPath("$.data[*].imageUrl", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[*].tags", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data[0].tags").value(hasItems(
                        "minimal"
                )))
                .andExpect(jsonPath("$.data[1].tags").value(hasItems(
                        "natural"
                )))
                .andExpect(jsonPath("$.data[2].tags").value(hasItems(
                        "modern"
                )))
                .andExpect(jsonPath("$.data[3].tags").value(hasItems("classic")))
                .andExpect(jsonPath("$.data[4].tags").value(hasItems("midcentury")));
    }
}
