package com.roomfit.placement;

import java.util.List;

public record FeedbackPlan(
        String version,
        FeedbackRequestKind requestKind,
        List<FeedbackOperation> operations,
        List<String> goals,
        FeedbackClarification clarification,
        String reason,
        FeedbackSource source,
        boolean fallbackUsed
) {
    public FeedbackPlan {
        operations = operations == null ? List.of() : List.copyOf(operations);
        goals = goals == null ? List.of() : List.copyOf(goals);
        reason = reason == null ? "" : reason.trim();
    }

    public boolean needsClarification() {
        return requestKind == FeedbackRequestKind.CLARIFICATION;
    }

    public String furnitureId() {
        return operations.isEmpty() || operations.getFirst().target() == null
                ? "" : operations.getFirst().target().furnitureId();
    }

    public String furnitureType() {
        return operations.isEmpty() || operations.getFirst().target() == null
                ? "" : operations.getFirst().target().furnitureType();
    }
}
