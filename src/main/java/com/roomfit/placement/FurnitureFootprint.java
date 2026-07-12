package com.roomfit.placement;

import com.roomfit.room.Furniture;

record FurnitureFootprint(double effectiveWidth, double effectiveDepth) {

    private static final double RIGHT_ANGLE_TOLERANCE = 1.0e-4;

    static FurnitureFootprint from(Furniture furniture) {
        return from(furniture.getWidth(), furniture.getDepth(), furniture.getRotation());
    }

    static FurnitureFootprint from(double width, double depth, double rotation) {
        double normalizedRotation = normalize(rotation);
        double nearestRightAngle = Math.rint(normalizedRotation / 90.0) * 90.0;

        if (Math.abs(normalizedRotation - nearestRightAngle) <= RIGHT_ANGLE_TOLERANCE) {
            int rightAngle = (int) Math.round(nearestRightAngle) % 360;
            if (rightAngle == 90 || rightAngle == 270) {
                return new FurnitureFootprint(depth, width);
            }
            return new FurnitureFootprint(width, depth);
        }

        double radians = Math.toRadians(normalizedRotation);
        double absoluteCosine = Math.abs(Math.cos(radians));
        double absoluteSine = Math.abs(Math.sin(radians));
        return new FurnitureFootprint(
                width * absoluteCosine + depth * absoluteSine,
                width * absoluteSine + depth * absoluteCosine
        );
    }

    private static double normalize(double rotation) {
        double normalized = rotation % 360.0;
        return normalized < 0 ? normalized + 360.0 : normalized;
    }
}
