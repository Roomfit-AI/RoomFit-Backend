package com.roomfit.placement;

import java.util.List;

public record FeedbackResult(
        boolean applied,
        FeedbackSource source,
        boolean fallbackUsed,
        String summary,
        List<String> operationsRequested,
        List<String> operationsApplied,
        String noChangeReason
) {
    public FeedbackResult {
        operationsRequested = List.copyOf(operationsRequested);
        operationsApplied = List.copyOf(operationsApplied);
    }
}
