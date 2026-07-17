package com.roomfit.docs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerUiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_returnsOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("RoomFit-Backend"))
                .andExpect(jsonPath("$.info.description", containsString("RoomFit MVP Demo Flow")))
                .andExpect(jsonPath("$.paths['/api/rooms/upload']").exists())
                .andExpect(jsonPath("$.paths['/api/products/mock']").exists());
    }

    @Test
    void apiDocs_containsFrontendFriendlyDescriptionsAndExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[?(@.name == 'Rooms')].description").exists())
                .andExpect(jsonPath("$.paths['/api/layouts/recommend'].post.summary").value("배치 추천 생성"))
                .andExpect(jsonPath("$.paths['/api/layouts/validate'].post.description", containsString("저장은 수행하지 않습니다")))
                .andExpect(jsonPath("$.paths['/api/agent/context'].post.requestBody.content['application/json'].examples['Study focused context'].value.selectedProductIds[0]").value("desk-01"))
                .andExpect(jsonPath("$.components.schemas.MockProductResponse.properties.purchaseUrl.format").value("uri"))
                .andExpect(jsonPath("$.components.schemas.MockProductResponse.properties.purchaseUrl.type").value(
                        containsInAnyOrder("string", "null")))
                .andExpect(jsonPath("$.components.schemas.MockProductResponse.properties.brand.type").value(
                        containsInAnyOrder("string", "null")))
                .andExpect(jsonPath("$.components.schemas.MockProductResponse.properties.price.type").value(
                        containsInAnyOrder("integer", "null")))
                .andExpect(jsonPath("$.components.schemas.MockProductResponse.properties.imageUrl.type").value(
                        containsInAnyOrder("string", "null")))
                .andExpect(jsonPath("$.components.schemas.MockProductResponse.properties.variantId.type").value(
                        containsInAnyOrder("string", "null")))
                .andExpect(jsonPath("$.components.schemas.MockProductResponse.properties.variantId.pattern")
                        .value("^[a-z0-9]+(?:-[a-z0-9]+)*$"))
                .andExpect(jsonPath("$.components.schemas.SelectedProductResponse.properties.variantId.type").value(
                        containsInAnyOrder("string", "null")))
                .andExpect(jsonPath("$.components.schemas.SelectedProductResponse.properties.variantId.pattern")
                        .value("^[a-z0-9]+(?:-[a-z0-9]+)*$"))
                .andExpect(jsonPath("$.components.schemas.Furniture.properties.variantId.type").value(
                        containsInAnyOrder("string", "null")))
                .andExpect(jsonPath("$.components.schemas.Furniture.properties.variantId.pattern")
                        .value("^[a-z0-9]+(?:-[a-z0-9]+)*$"))
                .andExpect(jsonPath("$.components.schemas.Furniture.description", containsString("x-z 평면 중심 좌표")))
                .andExpect(jsonPath("$.components.schemas.ValidationResult.properties.validationItems.description", containsString("체크리스트")));
    }

    @Test
    void swaggerUiIndex_isAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }
}
