package com.roomfit.placement;

/**
 * Overall outcome of a feedback plan. This is intentionally separate from the
 * existing recommendation status so older clients keep their current contract.
 */
public enum FeedbackStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    NEEDS_CLARIFICATION
}
