package com.roomfit.placement;

import java.util.List;

public record FeedbackPlan(
        String version,
        String furnitureId,
        String furnitureType,
        List<FeedbackOperation> operations,
        String reason,
        FeedbackSource source,
        boolean fallbackUsed
) {
    public FeedbackPlan {
        operations = operations == null ? List.of() : List.copyOf(operations);
    }

    public boolean needsClarification() {
        return furnitureId == null || furnitureId.isBlank();
    }
}
