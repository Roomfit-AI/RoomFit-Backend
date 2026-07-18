package com.roomfit.placement;

public record FeedbackOperation(
        FeedbackOperationType type,
        FeedbackDirection direction,
        Double distanceMeters,
        Integer rotationDegrees,
        FeedbackReplaceConstraints constraints
) {
}
