package com.roomfit.placement;

import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Opening;
import com.roomfit.room.Room;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    public ValidationResult validate(Room room, List<Furniture> furniture) {
        List<String> warnings = new ArrayList<>();
        List<Furniture> activeFurniture = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .toList();

        boolean collisionFree = checkCollisionFree(activeFurniture, warnings);
        boolean boundaryValid = checkBoundaryValid(room, activeFurniture, warnings);
        boolean doorClearance = checkOpeningClearance(room, activeFurniture, "door", warnings);
        boolean windowClearance = checkOpeningClearance(room, activeFurniture, "window", warnings);
        boolean pathSecured = checkPathSecured(room, activeFurniture, warnings);

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

    private boolean checkCollisionFree(List<Furniture> furniture, List<String> warnings) {
        for (int i = 0; i < furniture.size(); i++) {
            Rect current = Rect.from(furniture.get(i));
            for (int j = i + 1; j < furniture.size(); j++) {
                Rect other = Rect.from(furniture.get(j));
                if (current.overlaps(other)) {
                    warnings.add("가구 충돌이 감지되었습니다.");
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
