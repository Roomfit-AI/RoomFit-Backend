package com.roomfit.placement;

import java.util.List;

public record FeedbackOperation(
        String operationId,
        FeedbackOperationType type,
        FeedbackTargetSelector target,
        FeedbackPlacement placement,
        FeedbackReplaceConstraints constraints,
        List<String> dependsOn
) {
    public FeedbackOperation {
        operationId = operationId == null ? "" : operationId.trim();
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }
}
