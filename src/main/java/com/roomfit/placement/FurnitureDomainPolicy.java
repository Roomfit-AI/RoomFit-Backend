package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cross-entry-point policies that must hold for the final active furniture state.
 */
@Component
public class FurnitureDomainPolicy {

    static final String LOFT_DESK_VARIANT_ID = "bed-loft-desk";

    public void validateFinalState(List<Furniture> furniture) {
        List<Furniture> activeFurniture = furniture.stream()
                .filter(FurnitureDomainPolicy::isActive)
                .toList();
        boolean hasLoftDesk = activeFurniture.stream()
                .anyMatch(item -> LOFT_DESK_VARIANT_ID.equals(item.getVariantId()));
        boolean hasCanonicalDesk = activeFurniture.stream()
                .anyMatch(item -> isCanonicalType(item, "desk"));

        if (hasLoftDesk && hasCanonicalDesk) {
            throw new CustomException(ErrorCode.FURNITURE_DOMAIN_CONFLICT);
        }
    }

    public static boolean isRug(Furniture furniture) {
        return isCanonicalType(furniture, "rug");
    }

    static boolean isActive(Furniture furniture) {
        return furniture.getStatus() != FurnitureStatus.DELETED;
    }

    private static boolean isCanonicalType(Furniture furniture, String expectedType) {
        return expectedType.equals(GeneratedFurnitureCatalog.get().normalizeType(furniture.getType()));
    }
}
