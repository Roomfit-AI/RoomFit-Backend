package com.roomfit.placement;

public record FeedbackPlacement(
        FeedbackRelation relation,
        FeedbackMagnitude magnitude,
        FeedbackOrientation orientation,
        FeedbackSide side
) {
    public FeedbackPlacement(FeedbackRelation relation, FeedbackMagnitude magnitude,
                             FeedbackOrientation orientation) {
        this(relation, magnitude, orientation, null);
    }
}
