package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.service.MockProductService;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 규칙 기반 배치 추천 구현체 — LlmPlacementService의 폴백으로 쓰인다
 * (PlacementServiceConfig 참고). Spring 빈으로 자동 등록하지 않고
 * PlacementServiceConfig에서 직접 생성한다 — PlacementService 인터페이스의
 * 유일한 빈은 FallbackPlacementService여야 컨트롤러/서비스 쪽 주입이 모호해지지
 * 않는다 (FeedbackIntentParserConfig의 RuleBasedFeedbackIntentParser와 동일 패턴).
 *
 * TODO: 지금은 requiredItems를 단순 나열 배치하는 스켈레톤 수준.
 * 실제로는 lifestyleGoal/designStyle 별 배치 템플릿을 두고,
 * 방 크기 및 기존(existing) 가구와 겹치지 않는 템플릿을 선택하는 방식으로 구현 권장.
 */
public class RuleBasedPlacementService implements PlacementService {

    private static final Set<FurnitureStatus> ACTIVE_STATUSES = Set.of(
            FurnitureStatus.EXISTING,
            FurnitureStatus.USER_MODIFIED
    );

    private final MockProductService mockProductService;

    public RuleBasedPlacementService(MockProductService mockProductService) {
        this.mockProductService = mockProductService;
    }

    @Override
    public PlacementResult recommend(AgentContext context, Room room) {
        List<Furniture> recommended = new ArrayList<>();
        recommended.addAll(room.getFurniture().stream()
                .filter(this::isActivePlacedFurniture)
                .map(this::copyFurniture)
                .toList());

        // AgentContext는 selectedProductIds만 영속화한다 — 전체 MockProduct는
        // 여기서 다시 조회한다(AgentContext.java 참고).
        Map<String, MockProduct> selectedProductByType = mockProductService.findByProductIds(context.getSelectedProductIds()).stream()
                .collect(Collectors.toMap(MockProduct::getType, Function.identity(), (first, ignored) -> first));

        Set<String> placedTypes = recommended.stream()
                .map(Furniture::getType)
                .collect(Collectors.toSet());

        for (String itemType : context.getRequiredItems()) {
            if (placedTypes.contains(itemType)) {
                continue;
            }
            tryAddFurniture(room, recommended, itemType, selectedProductByType.get(itemType))
                    .ifPresent(furniture -> placedTypes.add(furniture.getType()));
        }

        for (String itemType : context.getOptionalItems()) {
            if (placedTypes.contains(itemType)) {
                continue;
            }
            tryAddFurniture(room, recommended, itemType, selectedProductByType.get(itemType))
                    .ifPresent(furniture -> placedTypes.add(furniture.getType()));
        }

        return new PlacementResult(RecommendationStatus.SUCCESS, recommended, ScoreSummary.defaultSummary());
    }

    private Optional<Furniture> tryAddFurniture(Room room, List<Furniture> placed,
                                                String itemType, MockProduct product) {
        FurnitureSpec spec = FurnitureSpec.from(itemType, product);
        for (Position position : candidatePositions(itemType, spec, room, placed)) {
            Furniture candidate = createRecommendedFurniture(itemType, spec, position);
            if (fitsInRoom(room, candidate) && doesNotCollide(placed, candidate)) {
                placed.add(candidate);
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Furniture createRecommendedFurniture(String itemType, FurnitureSpec spec, Position position) {
        return new Furniture(
                itemType + "-rec-1",
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

    private List<Position> candidatePositions(String itemType, FurnitureSpec spec,
                                               Room room, List<Furniture> placed) {
        return switch (itemType) {
            case "desk" -> List.of(
                    new Position(2.3, 1.0),
                    new Position(2.4, 1.4),
                    new Position(2.3, 2.0),
                    new Position(room.getWidth() - spec.width() / 2.0, 1.0)
            );
            case "chair" -> chairCandidatePositions(spec, placed);
            case "lamp" -> lampCandidatePositions(placed);
            case "storage" -> List.of(
                    new Position(2.7, 3.9),
                    new Position(0.6, 3.7),
                    new Position(2.6, 2.8)
            );
            case "rug" -> List.of(
                    new Position(1.1, 3.3),
                    new Position(1.8, 2.9),
                    new Position(1.6, 3.4)
            );
            case "bed" -> List.of(
                    new Position(0.8, 1.4),
                    new Position(0.8, 3.1),
                    new Position(1.1, 2.8)
            );
            default -> List.of(
                    new Position(2.2, 2.0),
                    new Position(1.6, 3.1),
                    new Position(0.8, 3.3)
            );
        };
    }

    private List<Position> chairCandidatePositions(FurnitureSpec spec, List<Furniture> placed) {
        return findPlacedByType(placed, "desk")
                .map(desk -> {
                    double frontZ = desk.getPosition().getZ() + desk.getDepth() / 2.0 + spec.depth() / 2.0 + 0.25;
                    double sideX = desk.getPosition().getX() - desk.getWidth() / 2.0 - spec.width() / 2.0 - 0.25;
                    return List.of(
                            new Position(desk.getPosition().getX(), frontZ),
                            new Position(sideX, desk.getPosition().getZ()),
                            new Position(desk.getPosition().getX(), desk.getPosition().getZ() + 1.0),
                            new Position(2.3, 1.8)
                    );
                })
                .orElse(List.of(
                        new Position(2.3, 1.8),
                        new Position(1.8, 2.7),
                        new Position(2.5, 2.2)
                ));
    }

    private List<Position> lampCandidatePositions(List<Furniture> placed) {
        return findPlacedByType(placed, "desk")
                .map(desk -> List.of(
                        new Position(desk.getPosition().getX() + desk.getWidth() / 2.0 + 0.15,
                                desk.getPosition().getZ()),
                        new Position(desk.getPosition().getX() - desk.getWidth() / 2.0 - 0.15,
                                desk.getPosition().getZ()),
                        new Position(desk.getPosition().getX(), desk.getPosition().getZ() + 0.65)
                ))
                .orElse(List.of(
                        new Position(2.0, 0.4),
                        new Position(2.9, 1.0),
                        new Position(1.7, 2.7)
                ));
    }

    private Optional<Furniture> findPlacedByType(List<Furniture> placed, String type) {
        return placed.stream()
                .filter(furniture -> type.equals(furniture.getType()))
                .findFirst();
    }

    private boolean isActivePlacedFurniture(Furniture furniture) {
        return ACTIVE_STATUSES.contains(furniture.getStatus());
    }

    private boolean fitsInRoom(Room room, Furniture candidate) {
        Rect rect = Rect.from(candidate);
        return rect.minX() >= 0
                && rect.maxX() <= room.getWidth()
                && rect.minZ() >= 0
                && rect.maxZ() <= room.getDepth();
    }

    private boolean doesNotCollide(List<Furniture> placed, Furniture candidate) {
        Rect candidateRect = Rect.from(candidate);
        return placed.stream()
                .filter(this::isCollisionTarget)
                .map(Rect::from)
                .noneMatch(candidateRect::overlaps);
    }

    private boolean isCollisionTarget(Furniture furniture) {
        return furniture.getStatus() != FurnitureStatus.DELETED;
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

    private record Rect(double minX, double maxX, double minZ, double maxZ) {

        private static Rect from(Furniture furniture) {
            double halfWidth = furniture.getWidth() / 2.0;
            double halfDepth = furniture.getDepth() / 2.0;
            return new Rect(
                    furniture.getPosition().getX() - halfWidth,
                    furniture.getPosition().getX() + halfWidth,
                    furniture.getPosition().getZ() - halfDepth,
                    furniture.getPosition().getZ() + halfDepth
            );
        }

        private boolean overlaps(Rect other) {
            return minX < other.maxX && maxX > other.minX
                    && minZ < other.maxZ && maxZ > other.minZ;
        }
    }
}
