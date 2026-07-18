package com.roomfit.placement;

import java.util.List;

public record FeedbackOperation(
        String operationId,
        FeedbackOperationType type,
        FeedbackTargetSelector target,
        FeedbackTargetSelector referenceTarget,
        FeedbackPlacement placement,
        FeedbackReplaceConstraints constraints,
        FeedbackProductRequirements productRequirements,
        FeedbackProductRequirements replacementRequirements,
        List<String> dependsOn
) {
    public FeedbackOperation(String operationId, FeedbackOperationType type,
                             FeedbackTargetSelector target, FeedbackPlacement placement,
                             FeedbackReplaceConstraints constraints, List<String> dependsOn) {
        this(operationId, type, target, null, placement, constraints, null, null, dependsOn);
    }

    public FeedbackOperation {
        operationId = operationId == null ? "" : operationId.trim();
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }
}
