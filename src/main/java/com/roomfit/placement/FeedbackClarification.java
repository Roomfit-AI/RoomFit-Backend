package com.roomfit.placement;

public record FeedbackClarification(String question, String targetFurnitureType) {
    public FeedbackClarification(String question) {
        this(question, "");
    }

    public FeedbackClarification {
        question = question == null ? "" : question.trim();
        targetFurnitureType = targetFurnitureType == null ? "" : targetFurnitureType.trim();
    }
}
