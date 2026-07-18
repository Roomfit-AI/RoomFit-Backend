package com.roomfit.placement.dto;

import java.util.List;

/** Information the client can use to ask the user for an unambiguous target. */
public record FeedbackClarificationResponse(
        String reasonCode,
        String question,
        String operationId,
        String requiredField,
        List<Candidate> candidates
) {
    public FeedbackClarificationResponse {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public record Candidate(String furnitureId, String type, String label) {
    }
}
