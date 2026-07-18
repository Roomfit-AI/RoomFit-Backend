package com.roomfit.placement;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.repository.MockProductRepository;
import com.roomfit.room.Furniture;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DeterministicFeedbackExecutor {

    private final ValidationService validationService;
    private final MockProductRepository productRepository;

    public DeterministicFeedbackExecutor(ValidationService validationService, MockProductRepository productRepository) {
        this.validationService = validationService;
        this.productRepository = productRepository;
    }

    public FeedbackExecution execute(FeedbackPlan plan, Room room, List<Furniture> original) {
        List<String> requested = plan.operations().stream().map(operation -> operation.type().name()).toList();
        if (plan.needsClarification()) {
            return noChange(original, plan, requested, "NEEDS_CLARIFICATION", "어떤 가구를 변경할지 알려주세요.");
        }
        if (plan.operations().isEmpty()) {
            return noChange(original, plan, requested, "UNSUPPORTED_OPERATION", "이번 피드백에서는 요청한 작업을 지원하지 않습니다.");
        }
        int targetIndex = targetIndex(plan, original);
        if (targetIndex < 0) {
            return noChange(original, plan, requested, "TARGET_FURNITURE_NOT_FOUND", "요청한 가구를 현재 배치에서 찾을 수 없습니다.");
        }

        List<Furniture> candidate = new ArrayList<>(original);
        List<String> applied = new ArrayList<>();
        String noChangeReason = null;
        for (FeedbackOperation operation : plan.operations()) {
            Furniture target = candidate.get(targetIndex);
            OperationAttempt attempt = switch (operation.type()) {
                case MOVE -> OperationAttempt.applied(move(room, candidate, targetIndex, operation));
                case ROTATE -> OperationAttempt.applied(rotate(room, candidate, targetIndex, operation));
                case REPLACE_PRODUCT -> replace(room, candidate, targetIndex, operation.constraints());
                case ADD_FURNITURE, REMOVE_FURNITURE -> OperationAttempt.notApplied("UNSUPPORTED_OPERATION");
            };
            if (attempt.furniture().isEmpty()) {
                noChangeReason = attempt.noChangeReason();
                continue;
            }
            candidate.set(targetIndex, attempt.furniture().get());
            applied.add(operation.type().name());
        }

        if (applied.isEmpty()) {
            String summary = summaryFor(noChangeReason);
            return noChange(original, plan, requested, noChangeReason == null ? "NO_CHANGE" : noChangeReason, summary);
        }
        return new FeedbackExecution(candidate, new FeedbackResult(true, plan.source(), plan.fallbackUsed(),
                "요청한 배치 변경을 적용했습니다.", requested, applied, null));
    }

    private int targetIndex(FeedbackPlan plan, List<Furniture> furniture) {
        for (int i = 0; i < furniture.size(); i++) {
            Furniture item = furniture.get(i);
            if (plan.furnitureId().equals(item.getId()) && (plan.furnitureType().isBlank() || plan.furnitureType().equals(item.getType()))) {
                return i;
            }
        }
        return -1;
    }

    private Optional<Furniture> move(Room room, List<Furniture> base, int index, FeedbackOperation operation) {
        Furniture current = base.get(index);
        List<Position> positions = movePositions(room, current, operation);
        for (Position position : positions) {
            Furniture updated = copy(current, position, current.getRotation());
            if (!samePosition(current, updated) && valid(room, replace(base, index, updated))) return Optional.of(updated);
        }
        return Optional.empty();
    }

    private List<Position> movePositions(Room room, Furniture item, FeedbackOperation operation) {
        double distance = operation.distanceMeters();
        FurnitureFootprint footprint = FurnitureFootprint.from(item);
        double x = item.getPosition().getX();
        double z = item.getPosition().getZ();
        Position delta = switch (operation.direction()) {
            case LEFT -> new Position(x - distance, z);
            case RIGHT -> new Position(x + distance, z);
            case FORWARD -> new Position(x, z - distance);
            case BACKWARD -> new Position(x, z + distance);
            case CENTER -> new Position(room.getWidth() / 2.0, room.getDepth() / 2.0);
            case NEAR_WALL -> new Position(room.getWidth() - footprint.effectiveWidth() / 2.0, z);
            case NEAR_WINDOW -> new Position(x, room.getDepth() - footprint.effectiveDepth() / 2.0);
            case AWAY_FROM_DOOR -> new Position(x, Math.min(room.getDepth() - footprint.effectiveDepth() / 2.0, z + distance));
        };
        return List.of(delta, clamp(room, delta, footprint));
    }

    private Optional<Furniture> rotate(Room room, List<Furniture> base, int index, FeedbackOperation operation) {
        Furniture current = base.get(index);
        double rotation = normalize(current.getRotation() + operation.rotationDegrees());
        if (rotation == current.getRotation()) return Optional.empty();
        Furniture updated = copy(current, current.getPosition(), rotation);
        return valid(room, replace(base, index, updated)) ? Optional.of(updated) : Optional.empty();
    }

    private OperationAttempt replace(Room room, List<Furniture> base, int index, FeedbackReplaceConstraints constraints) {
        Furniture current = base.get(index);
        if (!validReplaceConstraints(current, constraints)) {
            return OperationAttempt.notApplied("INVALID_REPLACE_CONSTRAINTS");
        }
        if (currentProductMatches(current, constraints)) {
            return OperationAttempt.notApplied("CURRENT_PRODUCT_ALREADY_MATCHES");
        }
        List<MockProduct> products = productRepository.findAll().stream()
                .filter(product -> current.getType().equals(product.getType()))
                .filter(product -> !product.getProductId().equals(current.getProductId()))
                .filter(product -> !constraints.largerThanCurrent() || product.getWidth() > current.getWidth())
                .filter(product -> constraints.minWidth() == null || product.getWidth() >= constraints.minWidth())
                .filter(product -> product.getStyleTags().containsAll(constraints.requiredStyleTags()))
                .filter(product -> product.getLifestyleTags().containsAll(constraints.requiredLifestyleTags()))
                .filter(product -> !constraints.storagePreferred() || hasStorage(product))
                .sorted(productComparator(constraints.storagePreferred()))
                .toList();
        if (products.isEmpty()) {
            return OperationAttempt.notApplied(constraints.largerThanCurrent()
                    ? "NO_LARGER_PRODUCT_AVAILABLE" : "NO_MATCHING_PRODUCT");
        }
        Optional<Furniture> placed = products.stream()
                .map(product -> replacement(room, base, index, current, product))
                .flatMap(Optional::stream)
                .findFirst();
        return placed.map(OperationAttempt::applied)
                .orElseGet(() -> OperationAttempt.notApplied("NO_VALID_PRODUCT_PLACEMENT"));
    }

    private boolean validReplaceConstraints(Furniture current, FeedbackReplaceConstraints constraints) {
        return constraints != null
                && constraints.furnitureType() != null
                && !constraints.furnitureType().isBlank()
                && current.getType().equals(constraints.furnitureType())
                && (constraints.largerThanCurrent()
                || constraints.minWidth() != null
                || !constraints.requiredStyleTags().isEmpty()
                || !constraints.requiredLifestyleTags().isEmpty()
                || constraints.storagePreferred());
    }

    private boolean currentProductMatches(Furniture current, FeedbackReplaceConstraints constraints) {
        if (current.getProductId() == null || constraints.largerThanCurrent()) {
            return false;
        }
        return productRepository.findById(current.getProductId())
                .filter(product -> constraints.minWidth() == null || product.getWidth() >= constraints.minWidth())
                .filter(product -> product.getStyleTags().containsAll(constraints.requiredStyleTags()))
                .filter(product -> product.getLifestyleTags().containsAll(constraints.requiredLifestyleTags()))
                .filter(product -> !constraints.storagePreferred() || hasStorage(product))
                .isPresent();
    }

    private Comparator<MockProduct> productComparator(boolean storagePreferred) {
        Comparator<MockProduct> byWidth = Comparator.comparingDouble(MockProduct::getWidth);
        if (!storagePreferred) return byWidth;
        return Comparator.comparing((MockProduct product) -> !hasStorage(product)).thenComparing(byWidth);
    }

    private boolean hasStorage(MockProduct product) {
        // STORAGE lifestyleTag alone is not sufficient: desk-corner also uses that tag for
        // recommendation scoring, but it is not a storage-desk replacement.  The catalog's
        // dedicated storage metadata is represented by the storage variant.
        return product.getVariantId() != null && product.getVariantId().contains("storage");
    }

    private Optional<Furniture> replacement(Room room, List<Furniture> base, int index, Furniture current, MockProduct product) {
        for (double rotation : replacementRotations(current.getRotation())) {
            Furniture prototype = new Furniture(current.getId(), product.getType(), product.getName(), product.getWidth(), product.getDepth(),
                    product.getHeight(), current.getPosition(), rotation, current.getStatus(), product.getProductId(),
                    product.getStyleTags(), product.getVariantId());
            for (Position position : replacementPositions(room, current.getPosition(), prototype)) {
                Furniture moved = copy(prototype, position, rotation);
                if (valid(room, replace(base, index, moved))) return Optional.of(moved);
            }
        }
        return Optional.empty();
    }

    private List<Double> replacementRotations(double currentRotation) {
        double quarterTurn = normalize(currentRotation + 90);
        return quarterTurn == currentRotation ? List.of(currentRotation) : List.of(currentRotation, quarterTurn);
    }

    private List<Position> replacementPositions(Room room, Position currentPosition, Furniture prototype) {
        FurnitureFootprint footprint = FurnitureFootprint.from(prototype);
        double halfWidth = footprint.effectiveWidth() / 2.0;
        double halfDepth = footprint.effectiveDepth() / 2.0;
        double currentX = currentPosition.getX();
        double currentZ = currentPosition.getZ();
        List<Position> positions = List.of(
                currentPosition,
                new Position(currentX - 0.25, currentZ), new Position(currentX + 0.25, currentZ),
                new Position(currentX, currentZ - 0.25), new Position(currentX, currentZ + 0.25),
                new Position(halfWidth, currentZ), new Position(room.getWidth() - halfWidth, currentZ),
                new Position(currentX, halfDepth), new Position(currentX, room.getDepth() - halfDepth)
        );
        return positions.stream().map(position -> clamp(room, position, footprint)).distinct().toList();
    }

    private String summaryFor(String noChangeReason) {
        if ("NO_LARGER_PRODUCT_AVAILABLE".equals(noChangeReason)) {
            return "현재 책상이 사용 가능한 제품 중 가장 넓어 기존 배치를 유지했습니다.";
        }
        if ("NO_VALID_PRODUCT_PLACEMENT".equals(noChangeReason)) {
            return "수납형 책상을 배치할 수 있는 유효한 위치를 찾지 못했습니다.";
        }
        if ("NO_MATCHING_PRODUCT".equals(noChangeReason)) {
            return "요청 조건에 맞는 대체 제품을 찾지 못했습니다.";
        }
        if ("CURRENT_PRODUCT_ALREADY_MATCHES".equals(noChangeReason)) {
            return "현재 제품이 이미 요청 조건을 만족해 기존 배치를 유지했습니다.";
        }
        if ("INVALID_REPLACE_CONSTRAINTS".equals(noChangeReason)) {
            return "제품 교체 조건을 안전하게 해석하지 못해 기존 배치를 유지했습니다.";
        }
        if ("UNSUPPORTED_OPERATION".equals(noChangeReason)) {
            return "이번 피드백에서는 요청한 작업을 지원하지 않습니다.";
        }
        return "요청을 안전하게 적용할 수 없어 기존 배치를 유지했습니다.";
    }

    private FeedbackExecution noChange(List<Furniture> original, FeedbackPlan plan, List<String> requested, String reason, String summary) {
        return new FeedbackExecution(original, new FeedbackResult(false, plan.source(), plan.fallbackUsed(), summary, requested, List.of(), reason));
    }

    private boolean valid(Room room, List<Furniture> furniture) {
        ValidationResult result = validationService.validate(room, furniture);
        return result.isCollisionFree() && result.isBoundaryValid() && result.isDoorClearance() && result.isWindowClearance() && result.isPathSecured();
    }

    private List<Furniture> replace(List<Furniture> furniture, int index, Furniture updated) {
        List<Furniture> copy = new ArrayList<>(furniture);
        copy.set(index, updated);
        return copy;
    }

    private Furniture copy(Furniture item, Position position, double rotation) {
        return new Furniture(item.getId(), item.getType(), item.getLabel(), item.getWidth(), item.getDepth(), item.getHeight(),
                position, rotation, item.getStatus(), item.getProductId(), item.getStyleTags(), item.getVariantId());
    }

    private boolean samePosition(Furniture first, Furniture second) {
        return first.getPosition().getX() == second.getPosition().getX() && first.getPosition().getZ() == second.getPosition().getZ();
    }

    private Position clamp(Room room, Position position, FurnitureFootprint footprint) {
        double x = Math.max(footprint.effectiveWidth() / 2.0, Math.min(room.getWidth() - footprint.effectiveWidth() / 2.0, position.getX()));
        double z = Math.max(footprint.effectiveDepth() / 2.0, Math.min(room.getDepth() - footprint.effectiveDepth() / 2.0, position.getZ()));
        return new Position(x, z);
    }

    private double normalize(double rotation) {
        double normalized = rotation % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    private record OperationAttempt(Optional<Furniture> furniture, String noChangeReason) {
        private static OperationAttempt applied(Optional<Furniture> furniture) {
            return furniture.map(value -> new OperationAttempt(Optional.of(value), null))
                    .orElseGet(() -> notApplied("OPERATION_NOT_APPLIED"));
        }

        private static OperationAttempt applied(Furniture furniture) {
            return new OperationAttempt(Optional.of(furniture), null);
        }

        private static OperationAttempt notApplied(String noChangeReason) {
            return new OperationAttempt(Optional.empty(), noChangeReason);
        }
    }
}
