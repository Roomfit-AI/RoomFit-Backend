package com.roomfit.placement.dto;

import com.roomfit.placement.FeedbackOperationType;

/** A deterministic, plan-order result for one feedback operation. */
public record FeedbackOperationResult(
        String operationId,
        FeedbackOperationType operationType,
        Status status,
        String reasonCode,
        String message,
        String targetFurnitureId,
        String resultFurnitureId,
        String productId,
        String variantId
) {
    public enum Status {
        APPLIED,
        FAILED,
        SKIPPED_DEPENDENCY,
        NEEDS_CLARIFICATION
    }
}
