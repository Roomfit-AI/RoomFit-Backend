package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;

import java.util.Map;

public class RuleBasedFeedbackIntentParser implements FeedbackIntentParser {

    @Override
    public FeedbackIntent parse(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        }

        return switch (feedback) {
            case "책상 더 크게" -> new FeedbackIntent(FeedbackIntentType.LARGER_DESK,
                    Map.of("deskMinWidth", 1.4));
            case "수납 늘려줘" -> new FeedbackIntent(FeedbackIntentType.STORAGE_PRIORITY,
                    Map.of("storagePriority", "HIGH"));
            case "방이 넓어 보이게" -> new FeedbackIntent(FeedbackIntentType.OPEN_SPACE_PRIORITY,
                    Map.of("openSpacePriority", "HIGH"));
            default -> throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        };
    }
}
