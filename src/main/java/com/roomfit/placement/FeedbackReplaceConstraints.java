package com.roomfit.placement;

import java.util.List;

public record FeedbackReplaceConstraints(
        String furnitureType,
        boolean largerThanCurrent,
        Double minWidth,
        List<String> requiredStyleTags,
        List<String> requiredLifestyleTags,
        boolean storagePreferred
) {
    public FeedbackReplaceConstraints {
        requiredStyleTags = requiredStyleTags == null ? List.of() : List.copyOf(requiredStyleTags);
        requiredLifestyleTags = requiredLifestyleTags == null ? List.of() : List.copyOf(requiredLifestyleTags);
    }
}
