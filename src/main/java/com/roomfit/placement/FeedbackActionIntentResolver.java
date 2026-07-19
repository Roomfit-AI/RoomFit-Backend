package com.roomfit.placement;

import java.util.List;

/** Shared, conservative action intent policy for rule and provider feedback plans. */
final class FeedbackActionIntentResolver {

    private static final List<String> MOVE_TERMS = List.of("옮겨", "옮기", "이동");
    private static final List<String> CREATION_TERMS = List.of("추가", "하나 더", "한 개 더");

    private FeedbackActionIntentResolver() {
    }

    static ActionIntent resolveFurnitureActionIntent(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            return ActionIntent.UNSPECIFIED;
        }
        // Movement remains the action even when it is modified by "추가로" or
        // quantified as "하나 더".
        if (containsAny(feedback, MOVE_TERMS)) {
            return ActionIntent.MOVE;
        }
        if (hasExplicitFurnitureCreationIntent(feedback)) {
            return ActionIntent.ADD;
        }
        return ActionIntent.UNSPECIFIED;
    }

    static boolean hasExplicitFurnitureCreationIntent(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            return false;
        }
        String compact = feedback.replaceAll("\\s+", "");
        if (compact.contains("추가로") || hasMoveAfter(compact, "하나더") || hasMoveAfter(compact, "한개더")) {
            return false;
        }
        return containsAny(feedback, CREATION_TERMS);
    }

    private static boolean hasMoveAfter(String feedback, String quantifiedCreationTerm) {
        int index = feedback.indexOf(quantifiedCreationTerm);
        return index >= 0 && containsAny(feedback.substring(index + quantifiedCreationTerm.length()), MOVE_TERMS);
    }

    private static boolean containsAny(String feedback, List<String> terms) {
        return terms.stream().anyMatch(feedback::contains);
    }

    enum ActionIntent {
        ADD,
        MOVE,
        UNSPECIFIED
    }
}
