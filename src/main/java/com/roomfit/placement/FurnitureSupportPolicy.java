package com.roomfit.placement;

import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.FurnitureStatus;

final class FurnitureSupportPolicy {

    static final double CENTER_EPSILON = 1.0e-6;

    private FurnitureSupportPolicy() {
    }

    static boolean isStrictStack(Furniture first, Furniture second) {
        SupportPair pair = SupportPair.resolve(first, second);
        if (pair == null || !active(pair.supporter()) || !active(pair.dependent())) return false;
        if (Math.abs(pair.supporter().getPosition().getX() - pair.dependent().getPosition().getX()) > CENTER_EPSILON
                || Math.abs(pair.supporter().getPosition().getZ() - pair.dependent().getPosition().getZ()) > CENTER_EPSILON) return false;
        FurnitureBoundary.LocalFootprint base = FurnitureBoundary.resolveLocalFootprint(
                pair.supporter().getWidth(), pair.supporter().getDepth(), pair.supporter().getVariantId());
        FurnitureBoundary.Footprint top = FurnitureBoundary.footprint(pair.dependent());
        double radians = Math.toRadians(pair.supporter().getRotation());
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        double offsetX = pair.dependent().getPosition().getX() - pair.supporter().getPosition().getX();
        double offsetZ = pair.dependent().getPosition().getZ() - pair.supporter().getPosition().getZ();
        return top.corners().stream().allMatch(corner -> {
            double worldX = offsetX + corner.x();
            double worldZ = offsetZ + corner.z();
            double localX = worldX * cosine + worldZ * sine;
            double localZ = -worldX * sine + worldZ * cosine;
            return localX >= base.minX() - CENTER_EPSILON
                    && localX <= base.maxX() + CENTER_EPSILON
                    && localZ >= base.minZ() - CENTER_EPSILON
                    && localZ <= base.maxZ() + CENTER_EPSILON;
        });
    }

    private static boolean active(Furniture furniture) {
        return furniture != null
                && furniture.getPosition() != null
                && furniture.getStatus() != FurnitureStatus.DELETED;
    }

    private record SupportPair(Furniture supporter, Furniture dependent) {

        private static SupportPair resolve(Furniture first, Furniture second) {
            if (first == null || second == null) return null;
            String firstType = GeneratedFurnitureCatalog.get().normalizeType(first.getType());
            String secondType = GeneratedFurnitureCatalog.get().normalizeType(second.getType());
            if (supported(firstType, secondType)) return new SupportPair(first, second);
            if (supported(secondType, firstType)) return new SupportPair(second, first);
            return null;
        }

        private static boolean supported(String supporterType, String dependentType) {
            return ("desk".equals(supporterType) && "monitor".equals(dependentType))
                    || ("media_console".equals(supporterType) && "tv".equals(dependentType));
        }
    }
}
