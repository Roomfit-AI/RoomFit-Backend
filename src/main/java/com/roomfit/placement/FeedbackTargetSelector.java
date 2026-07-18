package com.roomfit.placement;

public record FeedbackTargetSelector(
        String furnitureId,
        String furnitureType,
        String labelKeyword
) {
    public FeedbackTargetSelector {
        furnitureId = normalize(furnitureId);
        furnitureType = normalize(furnitureType);
        labelKeyword = normalize(labelKeyword);
    }

    public boolean isEmpty() {
        return furnitureId.isBlank() && furnitureType.isBlank() && labelKeyword.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
