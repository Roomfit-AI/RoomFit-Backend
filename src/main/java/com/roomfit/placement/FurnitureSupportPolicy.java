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
        FurnitureBoundary.Footprint base = FurnitureBoundary.footprint(pair.supporter());
        FurnitureBoundary.Footprint top = FurnitureBoundary.footprint(pair.dependent());
        return top.minX() >= base.minX() - CENTER_EPSILON
                && top.maxX() <= base.maxX() + CENTER_EPSILON
                && top.minZ() >= base.minZ() - CENTER_EPSILON
                && top.maxZ() <= base.maxZ() + CENTER_EPSILON;
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
