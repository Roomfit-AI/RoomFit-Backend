package com.roomfit.placement;

import java.util.List;

public record FeedbackProductRequirements(
        String furnitureType,
        FeedbackSizePreference sizePreference,
        boolean storagePreferred,
        List<String> styleKeywords
) {
    public FeedbackProductRequirements {
        furnitureType = furnitureType == null ? "" : furnitureType.trim();
        sizePreference = sizePreference == null ? FeedbackSizePreference.ANY : sizePreference;
        styleKeywords = styleKeywords == null ? List.of() : List.copyOf(styleKeywords);
    }
}
