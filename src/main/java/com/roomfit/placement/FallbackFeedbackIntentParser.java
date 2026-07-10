package com.roomfit.placement;

import com.roomfit.config.LlmFeedbackProperties;

public class FallbackFeedbackIntentParser implements FeedbackIntentParser {

    private final FeedbackIntentParser ruleBasedParser;
    private final LlmFeedbackProperties properties;

    public FallbackFeedbackIntentParser(FeedbackIntentParser ruleBasedParser,
                                         LlmFeedbackProperties properties) {
        this.ruleBasedParser = ruleBasedParser;
        this.properties = properties;
    }

    @Override
    public FeedbackIntent parse(String feedback) {
        // LLM parsing is intentionally not implemented yet. Even if the flag is
        // toggled early, the safe behavior remains the existing rule-based parser.
        return ruleBasedParser.parse(feedback);
    }

    public boolean isLlmEnabled() {
        return properties.isEnabled();
    }
}
