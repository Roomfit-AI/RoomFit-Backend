package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.service.MockProductService;
import com.roomfit.product.service.ProductRecommendationService;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    private static final String COLLECTOR_ROOM_NAME = "미드센추리 컬렉터 룸";
    private static final String COLLECTOR_STUDIO_ROOM_NAME = "샘플룸2";

    private final MockProductService mockProductService;
    private final ProductRecommendationService productRecommendationService;
    private final ValidationService validationService;

    public RuleBasedPlacementService(MockProductService mockProductService,
                                      ProductRecommendationService productRecommendationService) {
        this(mockProductService, productRecommendationService, new ValidationService());
    }

    RuleBasedPlacementService(MockProductService mockProductService,
                              ProductRecommendationService productRecommendationService,
                              ValidationService validationService) {
        this.mockProductService = mockProductService;
        this.productRecommendationService = productRecommendationService;
        this.validationService = validationService;
    }

    @Override
    public PlacementResult recommend(AgentContext context, Room room) {
        if (COLLECTOR_ROOM_NAME.equals(room.getName())) {
            return scriptedRecommendation(room, midCenturyCollectorRecommendation());
        }
        if (COLLECTOR_STUDIO_ROOM_NAME.equals(room.getName())) {
            return scriptedRecommendation(room, midCenturyStudioRecommendation());
        }

        List<Furniture> recommended = new ArrayList<>();
        recommended.addAll(room.getFurniture().stream()
                .filter(this::isActivePlacedFurniture)
                .map(this::copyFurniture)
                .toList());

        // AgentContext는 selectedProductIds만 영속화한다 — 전체 MockProduct는
        // 여기서 다시 조회한다(AgentContext.java 참고).
        Map<String, MockProduct> selectedProductByType = mockProductService.findByProductIds(context.getSelectedProductIds()).stream()
                .collect(Collectors.toMap(
                        product -> GeneratedFurnitureCatalog.get().normalizeType(product.getType()),
                        Function.identity(),
                        (first, ignored) -> first));

        // required는 optional보다 항상 먼저 시도한다 — lifestyleGoal 우선순위는 그
        // 원칙을 넘어서지 않고, 각 그룹 "안에서만" 더 관련 있는 타입을 앞으로
        // 당긴다(안정 정렬이라 우선순위가 같은 항목끼리는 사용자가 보낸 원래
        // 순서를 유지한다). 공간이나 12개 제한 때문에 전부 배치하지 못할 때,
        // 이 lifestyle과 더 관련된 항목이 먼저 배치를 시도하게 하기 위함이다.
        List<String> requiredItems = new ArrayList<>(context.getRequiredItems());
        List<String> optionalItems = new ArrayList<>(context.getOptionalItems());
        sortByLifestyleRelevance(requiredItems, context.getLifestyleGoal());
        sortByLifestyleRelevance(optionalItems, context.getLifestyleGoal());
        List<String> requestedItems = new ArrayList<>(requiredItems);
        requestedItems.addAll(optionalItems);
        List<UnplacedFurniture> unplaced = new ArrayList<>();
        int placedCount = 0;
        for (int index = 0; index < requestedItems.size(); index++) {
            String itemType = requestedItems.get(index);
            String canonicalItemType = GeneratedFurnitureCatalog.get().normalizeType(itemType);
            if (canonicalItemType == null || !supportedType(canonicalItemType)) {
                unplaced.add(unplaced(index, itemType, null, "UNSUPPORTED_FURNITURE_TYPE"));
                continue;
            }
            PlacementAttempt attempt = tryAddFurniture(room, recommended, context, canonicalItemType,
                    selectedProductByType.get(canonicalItemType));
            if (attempt.furniture() != null) {
                placedCount++;
            } else {
                unplaced.add(unplaced(index, canonicalItemType, attempt.product(), attempt.reasonCode()));
            }
        }
        RecommendationExecutionStatus outcome = placedCount == requestedItems.size()
                ? RecommendationExecutionStatus.SUCCESS
                : placedCount == 0 ? RecommendationExecutionStatus.FAILED : RecommendationExecutionStatus.PARTIAL_SUCCESS;
        String warningCode = unplaced.isEmpty() ? null : "INSUFFICIENT_ROOM_SPACE";
        String message = outcome == RecommendationExecutionStatus.SUCCESS
                ? "선택한 " + requestedItems.size() + "개 가구를 모두 배치했습니다."
                : outcome == RecommendationExecutionStatus.PARTIAL_SUCCESS
                ? "선택한 " + requestedItems.size() + "개 가구 중 " + placedCount + "개를 배치했습니다."
                : "선택한 가구를 안전하게 배치할 공간을 찾지 못했습니다.";
        return new PlacementResult(RecommendationStatus.SUCCESS, recommended, ScoreSummary.defaultSummary(),
                requestedItems.size(), placedCount, unplaced, outcome, warningCode, message);
    }

    /**
     * 사용자가 이미 보낸 항목들의 순서만 재배열한다 — 새 항목을 추가하지 않는다.
     * 안정 정렬이라 lifestyle 우선순위가 같은(또는 없는) 항목끼리는 원래 순서를
     * 유지한다.
     */
    private void sortByLifestyleRelevance(List<String> itemTypes, LifestyleGoal lifestyleGoal) {
        itemTypes.sort(Comparator.comparingInt(itemType ->
                LifestyleFurniturePriority.rank(lifestyleGoal, GeneratedFurnitureCatalog.get().normalizeType(itemType))));
    }

    private List<Furniture> midCenturyCollectorRecommendation() {
        return List.of(
                new Furniture("collector-bed", "bed", "미드센추리 싱글 침대", 1.35, 2.0, 0.5,
                        new Position(1.35, 1.8), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-bedside", "nightstand", "코랄 협탁", 0.48, 0.42, 0.52,
                        new Position(0.55, 1.7), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-floor-plant", "plant", "플로어 식물", 0.48, 0.48, 0.92,
                        new Position(0.58, 4.5), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-desk", "desk", "미드센추리 컬렉터 데스크", 1.35, 0.62, 0.74,
                        new Position(2.9, 1.0), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-desk-chair", "desk_chair", "월넛 데스크 체어", 0.58, 0.58, 0.82,
                        new Position(2.9, 1.95), 180, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-blue-cabinet", "drawer_chest", "코발트 모듈 수납장", 0.78, 0.42, 1.08,
                        new Position(1.35, 3.15), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-glass-shelf", "bookshelf", "크롬 글라스 전시 선반", 1.28, 0.36, 1.48,
                        new Position(3.95, 0.98), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-console", "media_console", "크림 LP 콘솔", 1.76, 0.44, 0.78,
                        new Position(5.08, 2.7), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-red-shelf", "partition_shelf", "레트로 레드 벽 선반", 0.92, 0.18, 0.22,
                        new Position(5.9, 2.3), 90, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-lounge-chair", "sofa", "코랄 라운지 체어", 0.92, 0.86, 0.84,
                        new Position(5.1, 4.55), 135, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-cane-chair", "desk_chair", "케인 크롬 체어", 0.68, 0.7, 0.82,
                        new Position(3.75, 4.6), 320, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-rug", "rug", "크림 라운드 러그", 2.2, 2.2, 0.035,
                        new Position(3.9, 4.08), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("collector-coffee-table", "multi_table", "글라스 커피 테이블", 0.9, 0.9, 0.42,
                        new Position(3.9, 4.08), 0, FurnitureStatus.RECOMMENDED)
        );
    }

    private List<Furniture> midCenturyStudioRecommendation() {
        return List.of(
                new Furniture("studio-bed", "bed", "코랄 프레임 침대", 1.35, 2.0, 0.5,
                        new Position(1.35, 1.8), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-bedside", "nightstand", "레드 미드센추리 협탁", 0.48, 0.42, 0.52,
                        new Position(0.55, 1.7), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-floor-plant", "plant", "윈도우 플로어 식물", 0.48, 0.48, 0.92,
                        new Position(0.58, 4.5), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-desk", "desk", "컬렉터 데스크", 1.35, 0.62, 0.74,
                        new Position(2.8, 1.05), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-desk-chair", "desk_chair", "월넛 데스크 체어", 0.58, 0.58, 0.82,
                        new Position(2.8, 1.9), 180, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-blue-cabinet", "drawer_chest", "코발트 모듈 수납장", 0.78, 0.42, 1.08,
                        new Position(5.55, 1.0), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-glass-shelf", "bookshelf", "크롬 글라스 전시 선반", 1.28, 0.36, 1.48,
                        new Position(4.45, 0.95), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-console", "media_console", "크림 LP 콘솔", 1.76, 0.44, 0.78,
                        new Position(5.08, 2.7), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-red-shelf", "partition_shelf", "레트로 레드 벽 선반", 0.92, 0.18, 0.22,
                        new Position(5.9, 2.3), 90, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-lounge-chair", "sofa", "코랄 라운지 체어", 0.92, 0.86, 0.84,
                        new Position(5.45, 4.75), 135, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-cane-chair", "desk_chair", "케인 크롬 체어", 0.68, 0.7, 0.82,
                        new Position(3.4, 5.25), 225, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-rug", "rug", "크림 라운드 러그", 2.2, 2.2, 0.035,
                        new Position(4.35, 4.3), 0, FurnitureStatus.RECOMMENDED),
                new Furniture("studio-coffee-table", "multi_table", "글라스 커피 테이블", 0.9, 0.9, 0.42,
                        new Position(4.35, 4.3), 0, FurnitureStatus.RECOMMENDED)
        );
    }

    private PlacementAttempt tryAddFurniture(Room room, List<Furniture> placed, AgentContext context,
                                             String itemType, MockProduct exactSelectedProduct) {
        // selectedProductIds에 이 타입의 정확한 선택이 있으면 그걸 그대로 쓴다(3번 우선순위 그대로 유지).
        // 없을 때만 ProductRecommendationService가 Catalog에서 하나를 고른다.
        MockProduct product = exactSelectedProduct != null
                ? exactSelectedProduct
                : productRecommendationService.recommend(itemType, context, room).orElse(null);
        if (exactSelectedProduct == null && product != null && product.getVariantId() == null) {
            // Preserve ProductRecommendationService's public legacy tie-break,
            // but initial AI layouts use a generated equivalent when available
            // so every canonical request carries renderer/footprint metadata.
            product = GeneratedFurnitureCatalog.get().products().stream()
                    .filter(candidate -> GeneratedFurnitureCatalog.get().sameType(itemType, candidate.getType()))
                    .findFirst().orElse(product);
        }
        if (product == null) {
            return new PlacementAttempt(null, null, "NO_RENDERABLE_PRODUCT");
        }
        FurnitureSpec spec = FurnitureSpec.from(itemType, product);
        // 시도한 모든 후보 위치의 실패 사유를 모아, 가장 흔했던(대표적인) 사유를
        // 반환한다 — 마지막으로 시도한 위치의 사유만 보면, 후보 20개 중 19개가
        // 충돌이었어도 우연히 마지막 하나가 경계 실패면 "경계 문제"로 잘못
        // 보고된다. 빈도가 같으면 validationReason()의 검사 순서(경계 -> 충돌 ->
        // 문 -> 창문 -> 동선)로 deterministic하게 tie-break한다.
        Map<String, Integer> reasonCounts = new LinkedHashMap<>();
        for (Position position : candidatePositions(itemType, spec, room, placed)) {
            Furniture prototype = createRecommendedFurniture(generateFurnitureId(itemType, placed), spec, position);
            Position safePosition = FurnitureBoundary.clamp(room, position, prototype).orElse(null);
            if (safePosition == null) {
                reasonCounts.merge("NO_VALID_BOUNDARY_PLACEMENT", 1, Integer::sum);
                continue;
            }
            Furniture candidate = createRecommendedFurniture(generateFurnitureId(itemType, placed), spec, safePosition);
            ValidationResult validation = validationService.validate(room, appended(placed, candidate));
            if (isHardValid(validation)) {
                placed.add(candidate);
                return new PlacementAttempt(candidate, product, null);
            }
            reasonCounts.merge(validationReason(validation), 1, Integer::sum);
        }
        return new PlacementAttempt(null, product, mostLikelyFailureReason(reasonCounts));
    }

    private Furniture createRecommendedFurniture(String id, FurnitureSpec spec, Position position) {
        return new Furniture(
                id,
                spec.type(),
                spec.label(),
                spec.width(),
                spec.depth(),
                spec.height(),
                position,
                0,
                FurnitureStatus.RECOMMENDED,
                spec.productId(),
                spec.styleTags(),
                spec.variantId()
        );
    }

    private List<Position> candidatePositions(String itemType, FurnitureSpec spec,
                                               Room room, List<Furniture> placed) {
        List<Position> preferred = switch (itemType) {
            case "desk" -> List.of(
                    new Position(2.3, 1.0),
                    new Position(2.4, 1.4),
                    new Position(2.3, 2.0),
                    new Position(room.getWidth() - spec.width() / 2.0, 1.0)
            );
            case "desk_chair" -> chairCandidatePositions(spec, placed);
            case "mood_lamp" -> lampCandidatePositions(placed);
            case "monitor" -> anchoredCandidatePositions(spec, placed, "desk", room);
            case "tv" -> anchoredCandidatePositions(spec, placed, "media_console", room);
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
            // 이전엔 방 크기와 무관한 절대 좌표 3개(2.2,2.0 / 1.6,3.1 / 0.8,3.3)를
            // 그대로 썼다 — 방이 비어있으면 첫 좌표가 clamp만 통과하면 바로 그
            // 자리에 배치돼, 작은 방에선 구석으로 눌리고 큰 방에선 방 크기와
            // 무관하게 늘 같은 좁은 구역에만 몰렸다(아래 4x4 grid fallback은 이
            // 좌표들이 전부 실패해야만 쓰이는데, 빈 방에서는 거의 항상 첫 좌표가
            // 그냥 통과해버려 grid까지 가지 않는다). 방 실측 치수에 비례한
            // 좌표로 바꿔 이 타입들(monitor/tv 외 아직 전용 규칙이 없는 나머지
            // 전부)도 방 크기를 반영하게 한다.
            default -> List.of(
                    new Position(room.getWidth() * 0.25, room.getDepth() * 0.25),
                    new Position(room.getWidth() * 0.75, room.getDepth() * 0.25),
                    new Position(room.getWidth() * 0.5, room.getDepth() * 0.75)
            );
        };
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Position> candidates = new ArrayList<>(preferred);
        double halfWidth = spec.width() / 2.0 + FurnitureBoundary.WALL_CLEARANCE_METERS;
        double halfDepth = spec.depth() / 2.0 + FurnitureBoundary.WALL_CLEARANCE_METERS;
        for (int xIndex = 0; xIndex < 4; xIndex++) {
            for (int zIndex = 0; zIndex < 4; zIndex++) {
                double x = halfWidth + (room.getWidth() - 2 * halfWidth) * xIndex / 3.0;
                double z = halfDepth + (room.getDepth() - 2 * halfDepth) * zIndex / 3.0;
                candidates.add(new Position(x, z));
            }
        }
        return candidates.stream().filter(position -> seen.add(position.getX() + ":" + position.getZ())).toList();
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

    /**
     * monitor를 desk 뒤쪽에, tv를 media_console 뒤쪽에 붙이기 위한 후보 좌표.
     * anchorType(desk/media_console)이 아직 배치되지 않았으면 방 크기 비례
     * 기본 좌표로 fallback한다.
     *
     * 주의: Position에는 높이(y)나 "무엇 위에 얹혔다"는 개념이 없어서, 실제로
     * "책상 위에 모니터가 올라간 것"을 표현하지는 못한다 — 할 수 있는 건 anchor
     * 가구의 뒤쪽 가장자리에 바짝 붙이는 것뿐이다. 게다가 지금 이 좌표들은
     * anchor와 겹치지 않게(뒤쪽으로 spec.depth()만큼 떨어뜨려) 잡았다 — 겹치는
     * 좌표를 후보로 넣어도 ValidationService의 충돌 검사(이번 변경 범위 밖)를
     * 통과하지 못해 항상 버려지기 때문이다. "진짜로 책상 위에 겹쳐 놓기"를
     * 하려면 rug처럼 충돌 예외를 인정하는 정책이 필요한데, 그건
     * FurnitureDomainPolicy/ValidationService 쪽 변경이 필요해 이번 실험
     * 범위에서는 보류했다.
     */
    private List<Position> anchoredCandidatePositions(FurnitureSpec spec, List<Furniture> placed,
                                                        String anchorType, Room room) {
        return findPlacedByType(placed, anchorType)
                .map(anchor -> {
                    double backZ = anchor.getPosition().getZ() - anchor.getDepth() / 2.0 - spec.depth() / 2.0 - 0.05;
                    return List.of(
                            new Position(anchor.getPosition().getX(), backZ),
                            new Position(anchor.getPosition().getX() + anchor.getWidth() / 2.0 - spec.width() / 2.0, backZ),
                            new Position(anchor.getPosition().getX() - anchor.getWidth() / 2.0 + spec.width() / 2.0, backZ)
                    );
                })
                .orElse(List.of(
                        new Position(room.getWidth() * 0.25, room.getDepth() * 0.25),
                        new Position(room.getWidth() * 0.75, room.getDepth() * 0.25),
                        new Position(room.getWidth() * 0.5, room.getDepth() * 0.75)
                ));
    }

    private Optional<Furniture> findPlacedByType(List<Furniture> placed, String type) {
        return placed.stream()
                .filter(furniture -> GeneratedFurnitureCatalog.get().sameType(type, furniture.getType()))
                .findFirst();
    }

    private boolean isActivePlacedFurniture(Furniture furniture) {
        return furniture.getStatus() != FurnitureStatus.DELETED;
    }

    private boolean fitsInRoom(Room room, Furniture candidate) {
        return FurnitureBoundary.isInside(room, candidate);
    }

    private boolean supportedType(String type) {
        return "storage".equals(type) || GeneratedFurnitureCatalog.get().products().stream()
                .anyMatch(product -> type.equals(product.getType()));
    }

    private List<Furniture> appended(List<Furniture> furniture, Furniture candidate) {
        List<Furniture> snapshot = new ArrayList<>(furniture);
        snapshot.add(candidate);
        return snapshot;
    }

    private boolean isHardValid(ValidationResult result) {
        return result.isCollisionFree() && result.isBoundaryValid() && result.isDoorClearance()
                && result.isWindowClearance() && result.isPathSecured();
    }

    private String validationReason(ValidationResult result) {
        if (!result.isBoundaryValid()) return "NO_VALID_BOUNDARY_PLACEMENT";
        if (!result.isCollisionFree()) return "COLLISION_DETECTED";
        if (!result.isDoorClearance()) return "DOOR_BLOCKED";
        if (!result.isWindowClearance()) return "WINDOW_BLOCKED";
        if (!result.isPathSecured()) return "MOVEMENT_PATH_BLOCKED";
        return "NO_VALID_PLACEMENT";
    }

    // validationReason()과 동일한 순서 — 빈도가 같을 때 이 순서로 deterministic하게
    // tie-break한다.
    private static final List<String> FAILURE_REASON_TIE_BREAK_ORDER = List.of(
            "NO_VALID_BOUNDARY_PLACEMENT", "COLLISION_DETECTED", "DOOR_BLOCKED",
            "WINDOW_BLOCKED", "MOVEMENT_PATH_BLOCKED", "NO_VALID_PLACEMENT");

    private String mostLikelyFailureReason(Map<String, Integer> reasonCounts) {
        if (reasonCounts.isEmpty()) {
            return "NO_VALID_PLACEMENT";
        }
        return reasonCounts.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(entry -> -FAILURE_REASON_TIE_BREAK_ORDER.indexOf(entry.getKey())))
                .map(Map.Entry::getKey)
                .orElse("NO_VALID_PLACEMENT");
    }

    private UnplacedFurniture unplaced(int index, String type, MockProduct product, String reasonCode) {
        return new UnplacedFurniture(index, type, product == null ? null : product.getProductId(),
                product == null ? null : product.getVariantId(), reasonCode, failureMessage(type, reasonCode));
    }

    private String failureMessage(String type, String reasonCode) {
        return switch (reasonCode) {
            case "UNSUPPORTED_FURNITURE_TYPE" -> "지원하지 않는 가구 유형입니다.";
            case "NO_RENDERABLE_PRODUCT" -> "렌더링 가능한 " + type + " 제품을 찾을 수 없습니다.";
            case "NO_VALID_BOUNDARY_PLACEMENT" -> type + "를 방 경계 안에 배치할 수 없습니다.";
            case "COLLISION_DETECTED" -> type + "가 기존 가구와 충돌합니다.";
            case "DOOR_BLOCKED" -> type + "가 문 앞 공간을 가립니다.";
            case "WINDOW_BLOCKED" -> type + "가 창문 앞 공간을 가립니다.";
            case "MOVEMENT_PATH_BLOCKED" -> type + "가 주요 이동 동선을 막습니다.";
            default -> type + "를 안전하게 배치할 공간이 부족합니다.";
        };
    }

    private String generateFurnitureId(String type, List<Furniture> furniture) {
        String prefix = type + "-rec-";
        int sequence = 1;
        Set<String> ids = furniture.stream().map(Furniture::getId).collect(Collectors.toSet());
        while (ids.contains(prefix + sequence)) sequence++;
        return prefix + sequence;
    }

    private boolean doesNotCollide(List<Furniture> placed, Furniture candidate) {
        if (FurnitureDomainPolicy.isRug(candidate)) {
            return true;
        }
        Rect candidateRect = Rect.from(candidate);
        return placed.stream()
                .filter(this::isCollisionTarget)
                .map(Rect::from)
                .noneMatch(candidateRect::overlaps);
    }

    private boolean isCollisionTarget(Furniture furniture) {
        return furniture.getStatus() != FurnitureStatus.DELETED
                && !FurnitureDomainPolicy.isRug(furniture);
    }

    private Furniture copyFurniture(Furniture furniture) {
        return copyFurniture(furniture, furniture.getPosition());
    }

    private PlacementResult scriptedRecommendation(Room room, List<Furniture> scriptedFurniture) {
        List<Furniture> furniture = new ArrayList<>(room.getFurniture().stream()
                .filter(this::isActivePlacedFurniture)
                .map(this::copyFurniture)
                .toList());
        Set<String> existingIds = furniture.stream().map(Furniture::getId).collect(Collectors.toSet());
        scriptedFurniture.stream()
                .filter(item -> !existingIds.contains(item.getId()))
                .map(item -> copyFurniture(item,
                        FurnitureBoundary.clamp(room, item.getPosition(), item).orElse(item.getPosition())))
                .forEach(furniture::add);

        ValidationResult validation = validationService.validate(room, furniture);
        RecommendationExecutionStatus outcome = isHardValid(validation)
                ? RecommendationExecutionStatus.SUCCESS
                : validation.isBoundaryValid() && !furniture.isEmpty()
                ? RecommendationExecutionStatus.PARTIAL_SUCCESS
                : RecommendationExecutionStatus.FAILED;
        String warningCode = outcome == RecommendationExecutionStatus.SUCCESS
                ? null : "LAYOUT_VALIDATION_FAILED";
        String message = switch (outcome) {
            case SUCCESS -> "고정 데모 배치가 모든 검증 조건을 통과했습니다.";
            case PARTIAL_SUCCESS -> "고정 데모 배치를 반환했지만 일부 검증 조건을 통과하지 못했습니다.";
            case FAILED -> "고정 데모 배치가 필수 검증 조건을 통과하지 못했습니다.";
        };
        return new PlacementResult(RecommendationStatus.SUCCESS, furniture, ScoreSummary.defaultSummary(),
                0, 0, List.of(), outcome, warningCode, message);
    }

    private Furniture copyFurniture(Furniture furniture, Position position) {
        return new Furniture(
                furniture.getId(),
                furniture.getType(),
                furniture.getLabel(),
                furniture.getWidth(),
                furniture.getDepth(),
                furniture.getHeight(),
                new Position(position.getX(), position.getZ()),
                furniture.getRotation(),
                furniture.getStatus(),
                furniture.getProductId(),
                furniture.getStyleTags(),
                furniture.getVariantId()
        );
    }

    private record FurnitureSpec(String type, String label, double width, double depth, double height,
                                 String productId, List<String> styleTags, String variantId) {

        private static FurnitureSpec from(String itemType, MockProduct product) {
            if (product != null) {
                return new FurnitureSpec(product.getType(), product.getName(), product.getWidth(), product.getDepth(),
                        product.getHeight(), product.getProductId(), product.getStyleTags(), product.getVariantId());
            }

            return switch (itemType) {
                case "bed" -> new FurnitureSpec("bed", "bed", 1.1, 2.0, 0.45, null, List.of(), null);
                case "desk" -> new FurnitureSpec("desk", "desk", 1.0, 0.6, 0.75, null, List.of(), null);
                case "chair" -> new FurnitureSpec("chair", "chair", 0.45, 0.45, 0.8, null, List.of(), null);
                case "storage" -> new FurnitureSpec("storage", "storage", 0.8, 0.4, 1.6, null, List.of(), null);
                case "rug" -> new FurnitureSpec("rug", "rug", 1.2, 1.6, 0.02, null, List.of(), null);
                case "lamp" -> new FurnitureSpec("lamp", "lamp", 0.25, 0.25, 1.4, null, List.of(), null);
                default -> new FurnitureSpec(itemType, itemType, 1.0, 0.6, 0.7, null, List.of(), null);
            };
        }
    }

    private record PlacementAttempt(Furniture furniture, MockProduct product, String reasonCode) {
    }

    private record Rect(double minX, double maxX, double minZ, double maxZ) {

        private static Rect from(Furniture furniture) {
            FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(furniture);
            return new Rect(
                    furniture.getPosition().getX() + footprint.minX(),
                    furniture.getPosition().getX() + footprint.maxX(),
                    furniture.getPosition().getZ() + footprint.minZ(),
                    furniture.getPosition().getZ() + footprint.maxZ()
            );
        }

        private boolean overlaps(Rect other) {
            return minX < other.maxX && maxX > other.minX
                    && minZ < other.maxZ && maxZ > other.minZ;
        }
    }
}
