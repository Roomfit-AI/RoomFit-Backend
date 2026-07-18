package com.roomfit.placement;

/** A single requested furniture instance that was not added to the snapshot. */
public record UnplacedFurniture(
        int requestIndex,
        String furnitureType,
        String productId,
        String variantId,
        String reasonCode,
        String message
) {
}
