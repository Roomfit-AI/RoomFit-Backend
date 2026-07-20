package com.roomfit.placement;

import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Opening;
import com.roomfit.room.Room;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 배치 검증 로직 (충돌 / 문 가림 / 창문 가림 / 동선 확보).
 * recommend와 validate/update 양쪽에서 재사용하는 별도 서비스로 분리.
 *
 * TODO: 지금은 스켈레톤 - 실제 충돌 판정(AABB 겹침 검사), 문/창문 앞 여유공간 계산,
 * 동선 확보(최소 통로 폭) 로직을 채워야 함.
 */
@Service
public class ValidationService {

    private static final double MIN_PATH_WIDTH = 0.6;
    private static final double DOOR_CLEARANCE_DEPTH = 0.8;
    private static final double DOOR_SIDE_MARGIN = 0.1;
    private static final double WINDOW_CLEARANCE_DEPTH = 0.4;
    private static final double WINDOW_SIDE_MARGIN = 0.1;
    private static final double WINDOW_ATTACHMENT_EPSILON = 1.0e-6;
    private static final String BASELINE_WARNING =
            "기존 가구의 선행 검증 문제는 신규 배치 평가에서 제외되었습니다.";

    public ValidationResult validate(Room room, List<Furniture> furniture) {
        List<String> warnings = new ArrayList<>();
        List<Furniture> activeFurniture = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .toList();
        List<Furniture> physicalObstacles = activeFurniture.stream()
                .filter(item -> !FurnitureDomainPolicy.isRug(item))
                .toList();

        boolean collisionFree = checkCollisionFree(physicalObstacles, warnings);
        boolean boundaryValid = checkBoundaryValid(room, activeFurniture, warnings);
        boolean doorClearance = checkOpeningClearance(room, physicalObstacles, "door", warnings);
        boolean windowClearance = checkOpeningClearance(room, physicalObstacles, "window", warnings);
        boolean pathSecured = checkPathSecured(room, physicalObstacles, warnings);

        return result(collisionFree, boundaryValid, doorClearance, windowClearance, pathSecured, warnings);
    }

    /**
     * Validates only safety debt introduced by the candidate snapshot. Uploaded
     * rooms can contain pre-existing overlaps, so unchanged baseline violations
     * must not prevent a safe new item from being added or make its score fail.
     */
    public ValidationResult validateChange(Room room, List<Furniture> baseline, List<Furniture> candidate) {
        List<Furniture> baselineItems = baseline == null ? List.of() : baseline;
        List<Furniture> candidateItems = candidate == null ? List.of() : candidate;
        List<Furniture> activeBaseline = active(baselineItems);
        List<Furniture> activeCandidate = active(candidateItems);
        Set<String> evaluatedIds = changedActiveIds(baselineItems, activeCandidate);
        List<Furniture> evaluated = activeCandidate.stream()
                .filter(item -> evaluatedIds.contains(item.getId()))
                .toList();
        List<Furniture> physicalCandidate = physical(activeCandidate);
        List<Furniture> physicalEvaluated = physical(evaluated);
        List<String> warnings = new ArrayList<>();

        boolean collisionFree = checkCollisionFreeForChanges(physicalCandidate, evaluatedIds, warnings);
        boolean boundaryValid = checkBoundaryValid(room, evaluated, warnings);
        boolean doorClearance = checkOpeningClearance(room, physicalEvaluated, "door", warnings);
        boolean windowClearance = checkOpeningClearance(room, physicalEvaluated, "window", warnings);

        List<String> ignoredWarnings = new ArrayList<>();
        boolean baselinePathSecured = checkPathSecured(room, physical(activeBaseline), ignoredWarnings);
        boolean pathSecured = baselinePathSecured
                ? checkPathSecured(room, physicalCandidate, warnings)
                : checkPathSecured(room, physicalEvaluated, warnings);

        ValidationResult baselineResult = validate(room, baselineItems);
        if (!hardValid(baselineResult)) {
            warnings.add(BASELINE_WARNING);
        }
        return result(collisionFree, boundaryValid, doorClearance, windowClearance, pathSecured, warnings);
    }

    boolean isSafeAddition(Room room, List<Furniture> baseline, Furniture added) {
        List<Furniture> candidate = new ArrayList<>(baseline);
        candidate.add(added);
        return hardValid(validateChange(room, baseline, candidate));
    }

    private ValidationResult result(boolean collisionFree, boolean boundaryValid,
                                    boolean doorClearance, boolean windowClearance,
                                    boolean pathSecured, List<String> warnings) {
        List<ValidationItem> validationItems = List.of(
                new ValidationItem("collision", collisionFree, collisionFree ? "가구 충돌 없음" : "가구 충돌 발생"),
                new ValidationItem("boundary", boundaryValid, boundaryValid ? "방 범위 내 배치" : "방 범위 밖 가구 존재"),
                new ValidationItem("door_clearance", doorClearance, doorClearance ? "문 앞 공간 확보" : "문 앞 공간 부족"),
                new ValidationItem("window_clearance", windowClearance, windowClearance ? "창문 앞 공간 확보" : "창문 앞 공간 부족"),
                new ValidationItem("path", pathSecured, pathSecured ? "이동 동선 확보" : "이동 동선 부족")
        );

        return new ValidationResult(collisionFree, boundaryValid, doorClearance,
                windowClearance, pathSecured, validationItems, warnings);
    }

    private List<Furniture> active(List<Furniture> furniture) {
        return furniture.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .toList();
    }

    private List<Furniture> physical(List<Furniture> furniture) {
        return furniture.stream()
                .filter(item -> !FurnitureDomainPolicy.isRug(item))
                .toList();
    }

    private Set<String> changedActiveIds(List<Furniture> baseline, List<Furniture> activeCandidate) {
        Map<String, Furniture> baselineById = new HashMap<>();
        for (Furniture item : baseline) {
            if (item != null && item.getId() != null) {
                baselineById.put(item.getId(), item);
            }
        }
        Set<String> changed = new HashSet<>();
        for (Furniture item : activeCandidate) {
            Furniture previous = baselineById.get(item.getId());
            if (previous == null || previous.getStatus() == FurnitureStatus.DELETED || !samePlacement(previous, item)) {
                changed.add(item.getId());
            }
        }
        return changed;
    }

    private boolean samePlacement(Furniture first, Furniture second) {
        if (first.getPosition() == null || second.getPosition() == null) return first.getPosition() == second.getPosition();
        return Objects.equals(first.getType(), second.getType())
                && Double.compare(first.getWidth(), second.getWidth()) == 0
                && Double.compare(first.getDepth(), second.getDepth()) == 0
                && Double.compare(first.getHeight(), second.getHeight()) == 0
                && Double.compare(first.getPosition().getX(), second.getPosition().getX()) == 0
                && Double.compare(first.getPosition().getZ(), second.getPosition().getZ()) == 0
                && Double.compare(first.getRotation(), second.getRotation()) == 0
                && first.getStatus() == second.getStatus();
    }

    private boolean hardValid(ValidationResult result) {
        return result.isCollisionFree() && result.isBoundaryValid() && result.isDoorClearance()
                && result.isWindowClearance() && result.isPathSecured();
    }

    boolean isSafeStandalonePlacement(Room room, Furniture furniture) {
        List<Furniture> standalone = List.of(furniture);
        List<String> warnings = new ArrayList<>();
        boolean boundaryValid = checkBoundaryValid(room, standalone, warnings);
        if (FurnitureDomainPolicy.isRug(furniture)) {
            return boundaryValid;
        }
        return boundaryValid
                && checkOpeningClearance(room, standalone, "door", warnings)
                && checkOpeningClearance(room, standalone, "window", warnings)
                && checkPathSecured(room, standalone, warnings);
    }

    private boolean checkCollisionFree(List<Furniture> furniture, List<String> warnings) {
        for (int i = 0; i < furniture.size(); i++) {
            Rect current = Rect.from(furniture.get(i));
            for (int j = i + 1; j < furniture.size(); j++) {
                if (FurnitureSupportPolicy.isStrictStack(furniture.get(i), furniture.get(j))) {
                    continue;
                }
                Rect other = Rect.from(furniture.get(j));
                if (current.overlaps(other)) {
                    warnings.add("가구 충돌이 감지되었습니다.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkCollisionFreeForChanges(List<Furniture> furniture, Set<String> evaluatedIds,
                                                 List<String> warnings) {
        for (int i = 0; i < furniture.size(); i++) {
            Furniture currentItem = furniture.get(i);
            Rect current = Rect.from(currentItem);
            for (int j = i + 1; j < furniture.size(); j++) {
                Furniture otherItem = furniture.get(j);
                if (!evaluatedIds.contains(currentItem.getId()) && !evaluatedIds.contains(otherItem.getId())) {
                    continue;
                }
                if (FurnitureSupportPolicy.isStrictStack(currentItem, otherItem)) {
                    continue;
                }
                if (current.overlaps(Rect.from(otherItem))) {
                    warnings.add("신규 또는 변경 가구 충돌이 감지되었습니다.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkBoundaryValid(Room room, List<Furniture> furniture, List<String> warnings) {
        for (Furniture item : furniture) {
            if (!FurnitureBoundary.isInside(room, item)) {
                warnings.add("방 범위를 벗어난 가구가 있습니다.");
                return false;
            }
        }
        return true;
    }

    private boolean checkOpeningClearance(Room room, List<Furniture> furniture,
                                           String openingType, List<String> warnings) {
        for (Opening opening : room.getOpenings()) {
            if (!openingType.equals(opening.getType())) {
                continue;
            }

            Rect clearance = clearanceRect(room, opening, openingType);
            for (Furniture item : furniture) {
                if ("window".equals(openingType) && isAttachedWindowTreatment(room, opening, item)) {
                    continue;
                }
                if ("window".equals(openingType) && item.getHeight() < windowSillHeight(opening)) {
                    continue;
                }
                if (clearance.contains(item.getPosition().getX(), item.getPosition().getZ())) {
                    warnings.add("door".equals(openingType) ? "문 앞 공간이 부족합니다." : "창문 앞 공간이 부족합니다.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isAttachedWindowTreatment(Room room, Opening opening, Furniture item) {
        if (!GeneratedFurnitureCatalog.get().sameType("curtain_blind", item.getType())) {
            return false;
        }
        FurnitureBoundary.UsableBounds usable = FurnitureBoundary.usableBounds(room).orElse(null);
        if (usable == null) return false;
        FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(item);
        double openingCenter = opening.getOffset() + opening.getWidth() / 2.0;
        return switch (opening.getWall()) {
            case "south" -> cardinalRotation(item, 0)
                    && near(item.getPosition().getZ() + footprint.minZ(), usable.minZ())
                    && alignedSpan(item.getPosition().getX(), openingCenter,
                    item.getPosition().getX() + footprint.minX(), item.getPosition().getX() + footprint.maxX(), opening);
            case "east" -> cardinalRotation(item, 90)
                    && near(item.getPosition().getX() + footprint.maxX(), usable.maxX())
                    && alignedSpan(item.getPosition().getZ(), openingCenter,
                    item.getPosition().getZ() + footprint.minZ(), item.getPosition().getZ() + footprint.maxZ(), opening);
            case "north" -> cardinalRotation(item, 180)
                    && near(item.getPosition().getZ() + footprint.maxZ(), usable.maxZ())
                    && alignedSpan(item.getPosition().getX(), openingCenter,
                    item.getPosition().getX() + footprint.minX(), item.getPosition().getX() + footprint.maxX(), opening);
            case "west" -> cardinalRotation(item, 270)
                    && near(item.getPosition().getX() + footprint.minX(), usable.minX())
                    && alignedSpan(item.getPosition().getZ(), openingCenter,
                    item.getPosition().getZ() + footprint.minZ(), item.getPosition().getZ() + footprint.maxZ(), opening);
            default -> false;
        };
    }

    private boolean alignedSpan(double itemCenter, double openingCenter,
                                double itemMin, double itemMax, Opening opening) {
        return near(itemCenter, openingCenter)
                && itemMax >= opening.getOffset() - WINDOW_ATTACHMENT_EPSILON
                && itemMin <= opening.getOffset() + opening.getWidth() + WINDOW_ATTACHMENT_EPSILON;
    }

    private boolean cardinalRotation(Furniture item, double expected) {
        double normalized = item.getRotation() % 360.0;
        if (normalized < 0) normalized += 360.0;
        return near(normalized, expected);
    }

    private boolean near(double first, double second) {
        return Math.abs(first - second) <= WINDOW_ATTACHMENT_EPSILON;
    }

    private boolean checkPathSecured(Room room, List<Furniture> furniture, List<String> warnings) {
        double centerX = room.getWidth() / 2.0;
        Rect path = new Rect(centerX - MIN_PATH_WIDTH / 2.0, centerX + MIN_PATH_WIDTH / 2.0,
                0, room.getDepth());

        for (Furniture item : furniture) {
            Rect rect = Rect.from(item);
            boolean blocksFullPathWidth = rect.minX() <= path.minX() && rect.maxX() >= path.maxX();
            boolean meaningfulDepth = rect.depth() >= room.getDepth() * 0.4;
            if (blocksFullPathWidth && meaningfulDepth) {
                warnings.add("이동 동선 폭이 부족합니다.");
                return false;
            }
        }
        return true;
    }

    private Rect clearanceRect(Room room, Opening opening, String openingType) {
        double depth = "door".equals(openingType) ? DOOR_CLEARANCE_DEPTH : WINDOW_CLEARANCE_DEPTH;
        double margin = "door".equals(openingType) ? DOOR_SIDE_MARGIN : WINDOW_SIDE_MARGIN;

        return switch (opening.getWall()) {
            case "north" -> new Rect(opening.getOffset() - margin,
                    opening.getOffset() + opening.getWidth() + margin,
                    room.getDepth() - depth, room.getDepth());
            case "south" -> new Rect(opening.getOffset() - margin,
                    opening.getOffset() + opening.getWidth() + margin,
                    0, depth);
            case "east" -> new Rect(room.getWidth() - depth, room.getWidth(),
                    opening.getOffset() - margin, opening.getOffset() + opening.getWidth() + margin);
            case "west" -> new Rect(0, depth,
                    opening.getOffset() - margin, opening.getOffset() + opening.getWidth() + margin);
            default -> new Rect(0, 0, 0, 0);
        };
    }

    private double windowSillHeight(Opening opening) {
        return opening.getSillHeight() == null ? 0 : opening.getSillHeight();
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

        private boolean contains(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        private double depth() {
            return maxZ - minZ;
        }
    }
}
