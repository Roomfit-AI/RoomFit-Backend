package com.roomfit.placement;

import java.util.Map;

public record FeedbackIntent(FeedbackIntentType type, Map<String, Object> interpretedIntent) {
}
