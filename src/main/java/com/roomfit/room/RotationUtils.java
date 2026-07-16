package com.roomfit.room;

import java.util.List;

/**
 * degree 단위 회전각을 0/90/180/270 중 가장 가까운 값으로 스냅한다.
 * RoomService(업로드)와 LlmPlacementService(LLM 응답) 양쪽에서 공용으로 사용.
 */
public final class RotationUtils {

    private RotationUtils() {
    }

    public static double snapToRightAngle(double rotation) {
        double normalized = ((rotation % 360) + 360) % 360;
        double nearest = 0;
        double shortestDistance = circularDistance(normalized, nearest);

        for (double allowed : List.of(90.0, 180.0, 270.0)) {
            double distance = circularDistance(normalized, allowed);
            if (distance < shortestDistance) {
                nearest = allowed;
                shortestDistance = distance;
            }
        }

        return nearest;
    }

    private static double circularDistance(double first, double second) {
        double distance = Math.abs(first - second);
        return Math.min(distance, 360 - distance);
    }
}
