package com.roomfit.placement;

public record FeedbackClarification(String question) {
    public FeedbackClarification {
        question = question == null ? "" : question.trim();
    }
}
