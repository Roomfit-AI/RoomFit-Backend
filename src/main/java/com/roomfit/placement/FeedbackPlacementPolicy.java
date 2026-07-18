package com.roomfit.placement;

import java.util.Comparator;
import java.util.List;

public final class FeedbackPlacementPolicy {

    private FeedbackPlacementPolicy() {
    }

    public static double movementDistance(FeedbackMagnitude magnitude) {
        return switch (magnitude) {
            case SMALL -> 0.2;
            case MEDIUM -> 0.4;
            case LARGE -> 0.6;
        };
    }

    public static List<Double> rotationCandidates(double currentRotation, FeedbackOrientation orientation) {
        double current = normalize(currentRotation);
        return switch (orientation) {
            case QUARTER_TURN_CW -> List.of(normalize(current + 90));
            case QUARTER_TURN_CCW -> List.of(normalize(current - 90));
            case HALF_TURN -> List.of(normalize(current + 180));
            case ALIGN_WITH_WALL -> List.of(0.0, 90.0, 180.0, 270.0).stream()
                    .sorted(Comparator.comparingDouble(candidate -> angularDistance(current, candidate)))
                    .toList();
        };
    }

    public static double normalize(double rotation) {
        double normalized = rotation % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    private static double angularDistance(double first, double second) {
        double distance = Math.abs(first - second) % 360;
        return Math.min(distance, 360 - distance);
    }
}
