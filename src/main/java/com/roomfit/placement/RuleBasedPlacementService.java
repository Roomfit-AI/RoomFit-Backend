package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 규칙 기반 배치 추천 구현체.
 *
 * TODO: 지금은 requiredItems를 단순 나열 배치하는 스켈레톤 수준.
 * 실제로는 lifestyleGoal/designStyle 별 배치 템플릿을 두고,
 * 방 크기 및 기존(existing) 가구와 겹치지 않는 템플릿을 선택하는 방식으로 구현 권장.
 * (본선에서 AI Agent 기반 구현체로 교체 시 PlacementService 인터페이스만 유지하면 됨)
 */
@Service
public class RuleBasedPlacementService implements PlacementService {

    @Override
    public PlacementResult recommend(AgentContext context, Room room) {
        List<Furniture> recommended = new ArrayList<>();
        recommended.addAll(room.getFurniture().stream()
                .filter(furniture -> furniture.getStatus() == FurnitureStatus.EXISTING)
                .map(this::copyFurniture)
                .toList());

        Map<String, MockProduct> selectedProductByType = context.getSelectedProducts().stream()
                .collect(Collectors.toMap(MockProduct::getType, Function.identity(), (first, ignored) -> first));

        List<String> placementItems = new ArrayList<>(context.getRequiredItems());
        placementItems.addAll(context.getOptionalItems());

        for (int index = 0; index < placementItems.size(); index++) {
            String itemType = placementItems.get(index);
            recommended.add(createRecommendedFurniture(itemType, index, selectedProductByType.get(itemType)));
        }

        return new PlacementResult(RecommendationStatus.SUCCESS, recommended, ScoreSummary.defaultSummary());
    }

    private Furniture createRecommendedFurniture(String itemType, int index, MockProduct product) {
        FurnitureSpec spec = FurnitureSpec.from(itemType, product);
        Position position = recommendedPosition(index);

        return new Furniture(
                itemType + "-rec-" + (index + 1),
                itemType,
                spec.label(),
                spec.width(),
                spec.depth(),
                spec.height(),
                position,
                0,
                FurnitureStatus.RECOMMENDED,
                spec.productId(),
                spec.styleTags()
        );
    }

    private Position recommendedPosition(int index) {
        List<Position> positions = List.of(
                new Position(2.2, 2.0),
                new Position(1.6, 3.1),
                new Position(1.1, 3.6),
                new Position(0.8, 3.3)
        );
        return positions.get(index % positions.size());
    }

    private Furniture copyFurniture(Furniture furniture) {
        return new Furniture(
                furniture.getId(),
                furniture.getType(),
                furniture.getLabel(),
                furniture.getWidth(),
                furniture.getDepth(),
                furniture.getHeight(),
                new Position(furniture.getPosition().getX(), furniture.getPosition().getZ()),
                furniture.getRotation(),
                furniture.getStatus(),
                furniture.getProductId(),
                furniture.getStyleTags()
        );
    }

    private record FurnitureSpec(String label, double width, double depth, double height,
                                 String productId, List<String> styleTags) {

        private static FurnitureSpec from(String itemType, MockProduct product) {
            if (product != null) {
                return new FurnitureSpec(product.getName(), product.getWidth(), product.getDepth(),
                        product.getHeight(), product.getProductId(), product.getStyleTags());
            }

            return switch (itemType) {
                case "bed" -> new FurnitureSpec("bed", 1.1, 2.0, 0.45, null, List.of());
                case "desk" -> new FurnitureSpec("desk", 1.0, 0.6, 0.75, null, List.of());
                case "chair" -> new FurnitureSpec("chair", 0.45, 0.45, 0.8, null, List.of());
                case "storage" -> new FurnitureSpec("storage", 0.8, 0.4, 1.6, null, List.of());
                case "rug" -> new FurnitureSpec("rug", 1.2, 1.6, 0.02, null, List.of());
                case "lamp" -> new FurnitureSpec("lamp", 0.25, 0.25, 1.4, null, List.of());
                default -> new FurnitureSpec(itemType, 1.0, 0.6, 0.7, null, List.of());
            };
        }
    }
}
