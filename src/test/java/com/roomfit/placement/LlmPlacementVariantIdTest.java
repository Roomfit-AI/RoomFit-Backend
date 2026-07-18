package com.roomfit.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.config.LlmFeedbackProperties;
import com.roomfit.llm.LlmClient;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import com.roomfit.product.service.MockProductService;
import com.roomfit.product.service.ProductRecommendationService;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmPlacementVariantIdTest {

    @Test
    void recommend_usesCatalogVariantIdAndIgnoresLlmVariantId() {
        MockProduct product = product("desk-compact");
        MockProductService productService = productService(product);
        AtomicReference<String> prompt = new AtomicReference<>();
        LlmClient client = value -> {
            prompt.set(value);
            return responseJson("invented-variant");
        };
        LlmPlacementService service = new LlmPlacementService(client, new ValidationService(),
                productService, new ObjectMapper());

        PlacementResult result = service.recommend(context(), room());

        assertThat(prompt.get()).contains("\"variantId\":\"desk-compact\"");
        assertThat(result.getRecommendedFurniture()).hasSize(1);
        Furniture furniture = result.getRecommendedFurniture().getFirst();
        assertThat(furniture.getProductId()).isEqualTo("desk-product");
        assertThat(furniture.getVariantId()).isEqualTo("desk-compact");
        assertThat(furniture.getType()).isEqualTo("desk");
        assertThat(furniture.getLabel()).isEqualTo("컴팩트 책상");
        assertThat(furniture.getWidth()).isEqualTo(1.2);
        assertThat(furniture.getDepth()).isEqualTo(0.6);
        assertThat(furniture.getHeight()).isEqualTo(0.73);
        assertThat(furniture.getStyleTags()).containsExactly("minimal");
        verify(productService).findByProductId("desk-product");
    }

    @Test
    void recommend_existingFurniturePreservesMatchingImmutableMetadata() {
        MockProduct productB = new MockProduct("product-b", "desk-storage", "desk", "다른 책상",
                "RoomFit Mock", 1.8, 0.8, 0.8, 99000, List.of("storage"),
                "/images/products/product-b.png", null, new RequiredClearance(0.6, 0.2));
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        when(productService.findByProductId("product-b")).thenReturn(productB);
        LlmPlacementService service = new LlmPlacementService(
                ignored -> existingResponseJson("product-a", "desk-compact"),
                new ValidationService(), productService, new ObjectMapper());

        PlacementResult result = service.recommend(contextWithoutProducts(), roomWithExistingDesk());

        Furniture furniture = result.getRecommendedFurniture().getFirst();
        assertExistingCatalogMetadata(furniture);
        assertThat(furniture.getPosition().getX()).isEqualTo(2.0);
        assertThat(furniture.getPosition().getZ()).isEqualTo(2.0);
        assertThat(furniture.getRotation()).isEqualTo(90);
        verify(productService, never()).findByProductId(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void recommend_rejectsExistingFurnitureProductOrVariantChanges() {
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        LlmPlacementService service = new LlmPlacementService(
                ignored -> existingResponseJson("unknown-product", "invented-variant"),
                new ValidationService(), productService, new ObjectMapper());

        assertThatThrownBy(() -> service.recommend(contextWithoutProducts(), roomWithExistingDesk()))
                .isInstanceOf(com.roomfit.common.CustomException.class);
        verify(productService, never()).findByProductId("unknown-product");
    }

    @Test
    void fallback_preservesRuleBasedVariantIdWhenLlmFails() {
        MockProduct product = product("desk-storage");
        MockProductService productService = productService(product);
        PlacementService failingLlm = (context, room) -> {
            throw new IllegalStateException("LLM unavailable");
        };
        FallbackPlacementService service = new FallbackPlacementService(
                Optional.of(failingLlm),
                new RuleBasedPlacementService(productService, mock(ProductRecommendationService.class)),
                new LlmFeedbackProperties());

        PlacementResult result = service.recommend(context(), room());

        assertThat(result.getStatus()).isEqualTo(RecommendationStatus.FALLBACK);
        assertThat(result.getRecommendedFurniture()).hasSize(1);
        assertThat(result.getRecommendedFurniture().getFirst().getVariantId()).isEqualTo("desk-storage");
    }

    @Test
    void recommend_newFurnitureWithoutCatalogMatch_usesLlmProposedSpecWithNullProductAndVariant() {
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        LlmPlacementService service = new LlmPlacementService(
                ignored -> newFurnitureWithoutProductResponseJson(),
                new ValidationService(), productService, new ObjectMapper());

        PlacementResult result = service.recommend(contextWithoutProducts("storage"), room());

        assertThat(result.getStatus()).isEqualTo(RecommendationStatus.SUCCESS);
        Furniture furniture = result.getRecommendedFurniture().getFirst();
        assertThat(furniture.getType()).isEqualTo("storage");
        assertThat(furniture.getLabel()).isEqualTo("LLM이 만든 새 수납장");
        assertThat(furniture.getWidth()).isEqualTo(0.8);
        assertThat(furniture.getDepth()).isEqualTo(0.4);
        assertThat(furniture.getHeight()).isEqualTo(1.2);
        assertThat(furniture.getProductId()).isNull();
        assertThat(furniture.getVariantId()).isNull();
        assertThat(furniture.getStyleTags()).containsExactly("llm-invented");
        verify(productService, never()).findByProductId(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void recommend_rejectsMissingOrDuplicateExistingFurnitureIds() {
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        LlmPlacementService missingService = new LlmPlacementService(
                ignored -> """
                        { "furniture": [{
                          "id":"new-desk", "type":"desk", "label":"new", "width":1.0, "depth":0.5, "height":0.7,
                          "position":{"x":2.0,"z":2.0}, "rotation":0, "status":"RECOMMENDED", "styleTags":[]
                        }] }
                        """, new ValidationService(), productService, new ObjectMapper());
        LlmPlacementService duplicateService = new LlmPlacementService(
                ignored -> """
                        { "furniture": [
                        %s,
                        %s ] }
                        """.formatted(existingFurnitureJson("product-a", "desk-compact", "EXISTING"),
                        existingFurnitureJson("product-a", "desk-compact", "EXISTING")),
                new ValidationService(), productService, new ObjectMapper());

        assertThatThrownBy(() -> missingService.recommend(contextWithoutProducts(), roomWithExistingDesk()))
                .isInstanceOf(com.roomfit.common.CustomException.class);
        assertThatThrownBy(() -> duplicateService.recommend(contextWithoutProducts(), roomWithExistingDesk()))
                .isInstanceOf(com.roomfit.common.CustomException.class);
    }

    @Test
    void recommend_rejectsExistingStatusOrShapeChanges() {
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        LlmPlacementService statusChange = new LlmPlacementService(
                ignored -> existingResponseJson("product-a", "desk-compact").replace("\"status\": \"EXISTING\"", "\"status\": \"USER_MODIFIED\""),
                new ValidationService(), productService, new ObjectMapper());
        LlmPlacementService dimensionChange = new LlmPlacementService(
                ignored -> existingResponseJson("product-a", "desk-compact").replace("\"width\": 1.2", "\"width\": 1.8"),
                new ValidationService(), productService, new ObjectMapper());

        assertThatThrownBy(() -> statusChange.recommend(contextWithoutProducts(), roomWithExistingDesk()))
                .isInstanceOf(com.roomfit.common.CustomException.class);
        assertThatThrownBy(() -> dimensionChange.recommend(contextWithoutProducts(), roomWithExistingDesk()))
                .isInstanceOf(com.roomfit.common.CustomException.class);
    }

    @Test
    void recommend_requiresExactCanonicalTypeInsteadOfSimilarTypeOrStorage() {
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        LlmPlacementService sofaBedRequest = new LlmPlacementService(
                ignored -> newFurnitureJson("bed", "bed-new"), new ValidationService(), productService, new ObjectMapper());

        assertThatThrownBy(() -> sofaBedRequest.recommend(contextWithoutProducts("sofa_bed"), room()))
                .isInstanceOf(com.roomfit.common.CustomException.class);
        for (String canonicalStorageType : List.of("bookshelf", "hanger", "partition_shelf", "wardrobe", "drawer_chest", "media_console")) {
            LlmPlacementService storageSubstitution = new LlmPlacementService(
                    ignored -> newFurnitureJson("storage", "storage-new-" + canonicalStorageType),
                    new ValidationService(), productService, new ObjectMapper());
            assertThatThrownBy(() -> storageSubstitution.recommend(contextWithoutProducts(canonicalStorageType), room()))
                    .as("storage must not fulfil %s", canonicalStorageType)
                    .isInstanceOf(com.roomfit.common.CustomException.class);
        }
    }

    private String newFurnitureWithoutProductResponseJson() {
        return """
                {
                  "furniture": [
                    {
                      "id": "storage-new-1",
                      "type": "storage",
                      "label": "LLM이 만든 새 수납장",
                      "width": 0.8,
                      "depth": 0.4,
                      "height": 1.2,
                      "position": { "x": 2.0, "z": 2.0 },
                      "rotation": 0,
                      "status": "RECOMMENDED",
                      "styleTags": ["llm-invented"]
                    }
                  ]
                }
                """;
    }

    private MockProductService productService(MockProduct product) {
        MockProductService service = mock(MockProductService.class);
        when(service.findByProductIds(List.of("desk-product"))).thenReturn(List.of(product));
        when(service.findByProductId("desk-product")).thenReturn(product);
        return service;
    }

    private AgentContext context() {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L), List.of("desk-product"), List.of("minimal"));
    }

    private AgentContext contextWithoutProducts() {
        return contextWithoutProducts("desk");
    }

    private AgentContext contextWithoutProducts(String type) {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of(type), List.of(), List.of(1L), List.of(), List.of("minimal"));
    }

    private Room room() {
        return new Room(null, 4.0, 4.0, 2.4, "meter", List.of(), List.of());
    }

    private Room roomWithExistingDesk() {
        Furniture desk = new Furniture("desk-1", "desk", "기존 책상", 1.2, 0.6, 0.73,
                new Position(1.0, 1.0), 0, FurnitureStatus.EXISTING,
                "product-a", List.of("original-style"), "desk-compact");
        return new Room(null, 4.0, 4.0, 2.4, "meter", List.of(), List.of(desk));
    }

    private void assertExistingCatalogMetadata(Furniture furniture) {
        assertThat(furniture.getId()).isEqualTo("desk-1");
        assertThat(furniture.getType()).isEqualTo("desk");
        assertThat(furniture.getLabel()).isEqualTo("기존 책상");
        assertThat(furniture.getWidth()).isEqualTo(1.2);
        assertThat(furniture.getDepth()).isEqualTo(0.6);
        assertThat(furniture.getHeight()).isEqualTo(0.73);
        assertThat(furniture.getProductId()).isEqualTo("product-a");
        assertThat(furniture.getVariantId()).isEqualTo("desk-compact");
        assertThat(furniture.getStyleTags()).containsExactly("original-style");
    }

    private MockProduct product(String variantId) {
        return new MockProduct("desk-product", variantId, "desk", "컴팩트 책상", "RoomFit Mock",
                1.2, 0.6, 0.73, 89000, List.of("minimal"), "/images/products/desk.png",
                null, new RequiredClearance(0.6, 0.2));
    }

    private String responseJson(String untrustedVariantId) {
        return """
                {
                  "furniture": [
                    {
                      "id": "desk-rec-1",
                      "type": "storage",
                      "label": "LLM이 만든 이름",
                      "width": 1.7,
                      "depth": 0.9,
                      "height": 1.1,
                      "position": { "x": 1.0, "z": 1.0 },
                      "rotation": 0,
                      "status": "RECOMMENDED",
                      "productId": "desk-product",
                      "variantId": "%s",
                      "styleTags": ["llm-invented"]
                    }
                  ]
                }
                """.formatted(untrustedVariantId);
    }

    private String existingResponseJson(String productId, String untrustedVariantId) {
        return """
                {
                  "furniture": [%s]
                }
                """.formatted(existingFurnitureJson(productId, untrustedVariantId, "EXISTING"));
    }

    private String existingFurnitureJson(String productId, String variantId, String status) {
        return """
                {
                  "id": "desk-1",
                  "type": "desk",
                  "label": "기존 책상",
                  "width": 1.2,
                  "depth": 0.6,
                  "height": 0.73,
                  "position": { "x": 2.0, "z": 2.0 },
                  "rotation": 90,
                  "status": "%s",
                  "productId": "%s",
                  "variantId": "%s",
                  "styleTags": ["original-style"]
                }
                """.formatted(status, productId, variantId);
    }

    private String newFurnitureJson(String type, String id) {
        return """
                { "furniture": [{
                  "id":"%s", "type":"%s", "label":"LLM furniture", "width":1.0, "depth":0.5, "height":0.7,
                  "position":{"x":2.0,"z":2.0}, "rotation":0, "status":"RECOMMENDED", "styleTags":[]
                }] }
                """.formatted(id, type);
    }
}
