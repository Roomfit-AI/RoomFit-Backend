package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuleBasedPlacementVariantIdTest {

    @Test
    void recommend_preservesSelectedProductVariantId() {
        MockProduct product = product("desk-compact");
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of("desk-product"))).thenReturn(List.of(product));

        PlacementResult result = new RuleBasedPlacementService(productService, mock(ProductRecommendationService.class))
                .recommend(context(), room());

        assertThat(result.getRecommendedFurniture()).hasSize(1);
        Furniture furniture = result.getRecommendedFurniture().getFirst();
        assertThat(furniture.getProductId()).isEqualTo("desk-product");
        assertThat(furniture.getVariantId()).isEqualTo("desk-compact");
    }

    @Test
    void recommend_keepsNullVariantIdCompatible() {
        MockProduct product = product(null);
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of("desk-product"))).thenReturn(List.of(product));

        PlacementResult result = new RuleBasedPlacementService(productService, mock(ProductRecommendationService.class))
                .recommend(context(), room());

        assertThat(result.getRecommendedFurniture()).hasSize(1);
        assertThat(result.getRecommendedFurniture().getFirst().getVariantId()).isNull();
    }

    @Test
    void recommend_usesProductRecommendationService_whenNoExactSelectedProductForType() {
        MockProduct recommended = product("desk-corner");
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        ProductRecommendationService productRecommendationService = mock(ProductRecommendationService.class);
        when(productRecommendationService.recommend(eq("desk"), any(AgentContext.class), any(Room.class)))
                .thenReturn(Optional.of(recommended));

        AgentContext context = new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L), List.of(), List.of("minimal"));

        PlacementResult result = new RuleBasedPlacementService(productService, productRecommendationService)
                .recommend(context, room());

        assertThat(result.getRecommendedFurniture()).hasSize(1);
        Furniture furniture = result.getRecommendedFurniture().getFirst();
        assertThat(furniture.getProductId()).isEqualTo("desk-product");
        assertThat(furniture.getVariantId()).isEqualTo("desk-corner");
    }

    @Test
    void recommend_skipsProductRecommendation_whenTypeAlreadyPlacedInRoom() {
        MockProductService productService = mock(MockProductService.class);
        when(productService.findByProductIds(List.of())).thenReturn(List.of());
        ProductRecommendationService productRecommendationService = mock(ProductRecommendationService.class);

        Furniture existingDesk = new Furniture("existing-desk", "desk", "기존 책상", 1.2, 0.6, 0.72,
                new Position(2.0, 1.0), 0, FurnitureStatus.EXISTING);
        Room room = new Room(null, 4.0, 4.0, 2.4, "meter", List.of(), List.of(existingDesk));

        AgentContext context = new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L), List.of(), List.of("minimal"));

        PlacementResult result = new RuleBasedPlacementService(productService, productRecommendationService)
                .recommend(context, room);

        assertThat(result.getRecommendedFurniture()).hasSize(1);
        assertThat(result.getRecommendedFurniture().getFirst().getId()).isEqualTo("existing-desk");
        verify(productRecommendationService, never()).recommend(any(), any(), any());
    }

    private AgentContext context() {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L), List.of("desk-product"), List.of("minimal"));
    }

    private Room room() {
        return new Room(null, 4.0, 4.0, 2.4, "meter", List.of(), List.of());
    }

    private MockProduct product(String variantId) {
        return new MockProduct("desk-product", variantId, "desk", "컴팩트 책상", "RoomFit Mock",
                1.2, 0.6, 0.73, 89000, List.of("minimal"), "/images/products/desk.png",
                null, new RequiredClearance(0.6, 0.2));
    }
}
