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
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
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
    void recommend_existingFurnitureIgnoresDifferentCatalogProductMetadata() {
        MockProduct productB = new MockProduct("product-b", "desk-storage", "desk", "다른 책상",
                "RoomFit Mock", 1.8, 0.8, 0.8, 99000, List.of("storage"),
                "/images/products/product-b.png", null, new RequiredClearance(0.6, 0.2));
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        when(productService.findByProductId("product-b")).thenReturn(productB);
        LlmPlacementService service = new LlmPlacementService(
                ignored -> existingResponseJson("product-b", "invented-variant"),
                new ValidationService(), productService, new ObjectMapper());

        PlacementResult result = service.recommend(contextWithoutProducts(), roomWithExistingDesk());

        Furniture furniture = result.getRecommendedFurniture().getFirst();
        assertExistingCatalogMetadata(furniture);
        assertThat(furniture.getPosition().getX()).isEqualTo(2.0);
        assertThat(furniture.getPosition().getZ()).isEqualTo(2.0);
        assertThat(furniture.getRotation()).isEqualTo(90);
        verify(productService, never()).findByProductId("product-b");
    }

    @Test
    void recommend_existingFurnitureIgnoresUnknownProductIdWithoutFallback() {
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        LlmPlacementService service = new LlmPlacementService(
                ignored -> existingResponseJson("unknown-product", "invented-variant"),
                new ValidationService(), productService, new ObjectMapper());

        PlacementResult result = service.recommend(contextWithoutProducts(), roomWithExistingDesk());

        assertThat(result.getStatus()).isEqualTo(RecommendationStatus.SUCCESS);
        assertExistingCatalogMetadata(result.getRecommendedFurniture().getFirst());
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
                Optional.of(failingLlm), new RuleBasedPlacementService(productService), new LlmFeedbackProperties());

        PlacementResult result = service.recommend(context(), room());

        assertThat(result.getStatus()).isEqualTo(RecommendationStatus.FALLBACK);
        assertThat(result.getRecommendedFurniture()).hasSize(1);
        assertThat(result.getRecommendedFurniture().getFirst().getVariantId()).isEqualTo("desk-storage");
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
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L), List.of(), List.of("minimal"));
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
                  "furniture": [
                    {
                      "id": "desk-1",
                      "type": "storage",
                      "label": "LLM이 바꾼 이름",
                      "width": 1.8,
                      "depth": 0.8,
                      "height": 0.8,
                      "position": { "x": 2.0, "z": 2.0 },
                      "rotation": 90,
                      "status": "USER_MODIFIED",
                      "productId": "%s",
                      "variantId": "%s",
                      "styleTags": ["llm-invented"]
                    }
                  ]
                }
                """.formatted(productId, untrustedVariantId);
    }
}
