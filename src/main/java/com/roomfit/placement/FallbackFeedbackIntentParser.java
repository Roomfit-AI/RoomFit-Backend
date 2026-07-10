package com.roomfit.placement;

import com.roomfit.config.LlmFeedbackProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class FallbackFeedbackIntentParser implements FeedbackIntentParser {

    private final Optional<FeedbackIntentParser> primaryParser;
    private final FeedbackIntentParser ruleBasedParser;
    private final LlmFeedbackProperties properties;

    public FallbackFeedbackIntentParser(Optional<FeedbackIntentParser> primaryParser,
                                         FeedbackIntentParser ruleBasedParser,
                                         LlmFeedbackProperties properties) {
        this.primaryParser = primaryParser;
        this.ruleBasedParser = ruleBasedParser;
        this.properties = properties;
    }

    @Override
    public FeedbackIntent parse(String feedback) {
        if (primaryParser.isEmpty()) {
            return ruleBasedParser.parse(feedback);
        }

        try {
            return primaryParser.get().parse(feedback);
        } catch (RuntimeException ignored) {
            return withFallbackMetadata(ruleBasedParser.parse(feedback));
        }
    }

    public boolean isLlmEnabled() {
        return properties.isEnabled();
    }

    public boolean hasPrimaryParser() {
        return primaryParser.isPresent();
    }

    private FeedbackIntent withFallbackMetadata(FeedbackIntent fallbackIntent) {
        Map<String, Object> interpretedIntent = new LinkedHashMap<>(fallbackIntent.interpretedIntent());
        interpretedIntent.put("source", "RULE_BASED");
        interpretedIntent.put("fallbackUsed", true);
        return new FeedbackIntent(fallbackIntent.type(), interpretedIntent);
    }
}
