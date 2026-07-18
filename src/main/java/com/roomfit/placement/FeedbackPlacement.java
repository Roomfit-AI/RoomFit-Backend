package com.roomfit.placement;

public record FeedbackPlacement(
        FeedbackRelation relation,
        FeedbackMagnitude magnitude,
        FeedbackOrientation orientation
) {
}
