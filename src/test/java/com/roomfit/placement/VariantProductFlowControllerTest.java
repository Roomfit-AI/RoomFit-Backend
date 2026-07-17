package com.roomfit.placement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seededVariantProduct_reachesAgentRecommendUpdateFeedbackConfirmAndRoom() throws Exception {
        mockMvc.perform(get("/api/products/mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.productId == 'desk-compact-01')].variantId")
                        .value(hasItems("desk-compact")));

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
                                  "selectedProductIds": ["desk-compact-01"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.selectedProducts[0].productId").value("desk-compact-01"))
                .andExpect(jsonPath("$.data.selectedProducts[0].variantId").value("desk-compact"))
                .andExpect(jsonPath("$.data.selectedProducts[0].width").value(1.2))
                .andExpect(jsonPath("$.data.selectedProducts[0].depth").value(0.6))
                .andExpect(jsonPath("$.data.selectedProducts[0].height").value(0.73))
                .andExpect(jsonPath("$.data.selectedProducts[0].styleTags").value(hasItems("minimal", "classic")))
                .andReturn().getResponse().getContentAsString();

        Integer contextId = JsonPath.read(contextResponse, "$.data.contextId");

        String recommendResponse = mockMvc.perform(post("/api/layouts/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "contextId": %d }
                                """.formatted(contextId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendedFurniture[*].productId").value(hasItems("desk-compact-01")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.productId == 'desk-compact-01')].variantId")
                        .value(hasItems("desk-compact")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.productId == 'desk-compact-01')].width")
                        .value(hasItems(1.2)))
                .andReturn().getResponse().getContentAsString();

        Integer layoutId = JsonPath.read(recommendResponse, "$.data.layoutId");
        String updateResponse = mockMvc.perform(put("/api/layouts/{layoutId}", layoutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(recommendResponse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.productId == 'desk-compact-01')].variantId")
                        .value(hasItems("desk-compact")))
                .andReturn().getResponse().getContentAsString();

        Integer updatedLayoutId = JsonPath.read(updateResponse, "$.data.layoutId");
        String feedbackResponse = mockMvc.perform(post("/api/layouts/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutId": %d,
                                  "feedback": "방이 넓어 보이게"
                                }
                                """.formatted(updatedLayoutId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.productId == 'desk-compact-01')].variantId")
                        .value(hasItems("desk-compact")))
                .andExpect(jsonPath("$.data.recommendedFurniture[?(@.productId == 'desk-compact-01')].width")
                        .value(hasItems(1.2)))
                .andReturn().getResponse().getContentAsString();

        Integer feedbackLayoutId = JsonPath.read(feedbackResponse, "$.data.layoutId");
        mockMvc.perform(post("/api/layouts/{layoutId}/confirm", feedbackLayoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmed").value(true));

        String roomResponse = mockMvc.perform(get("/api/rooms/{roomId}", 1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> furniture = JsonPath.read(roomResponse, "$.data.furniture");
        Map<String, Object> desk = furniture.stream()
                .filter(item -> "desk-compact-01".equals(item.get("productId")))
                .findFirst()
                .orElseThrow();
        assertThat(desk.get("variantId")).isEqualTo("desk-compact");
        assertThat(((Number) desk.get("width")).doubleValue()).isEqualTo(1.2);
        assertThat(((Number) desk.get("depth")).doubleValue()).isEqualTo(0.6);
        assertThat(((Number) desk.get("height")).doubleValue()).isEqualTo(0.73);
        assertThat(desk.get("styleTags")).isEqualTo(List.of("minimal", "classic"));
    }

    private String updateBody(String recommendResponse) throws Exception {
        JsonNode furniture = objectMapper.readTree(recommendResponse).at("/data/recommendedFurniture");
        ObjectNode request = objectMapper.createObjectNode();
        ArrayNode updates = request.putArray("furniture");

        furniture.forEach(item -> {
            ObjectNode update = updates.addObject();
            update.put("id", item.path("id").asText());
            update.set("position", item.path("position"));
            update.put("rotation", item.path("rotation").asDouble());
            update.put("status", item.path("status").asText());
        });
        return objectMapper.writeValueAsString(request);
    }
}
