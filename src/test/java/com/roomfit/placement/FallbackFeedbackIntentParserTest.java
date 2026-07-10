package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.config.LlmFeedbackProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackFeedbackIntentParserTest {

    @Test
    void parse_withPrimaryParserSuccess_returnsPrimaryIntent() {
        FeedbackIntentParser primaryParser = feedback -> new FeedbackIntent(
                FeedbackIntentType.LARGER_DESK,
                Map.of("deskMinWidth", 1.5, "source", "LLM", "fallbackUsed", false)
        );
        FallbackFeedbackIntentParser parser = new FallbackFeedbackIntentParser(
                Optional.of(primaryParser),
                new RuleBasedFeedbackIntentParser(),
                enabledProperties()
        );

        FeedbackIntent intent = parser.parse("책상 더 크게");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.LARGER_DESK);
        assertThat(intent.interpretedIntent())
                .containsEntry("deskMinWidth", 1.5)
                .containsEntry("source", "LLM")
                .containsEntry("fallbackUsed", false);
    }

    @Test
    void parse_withPrimaryParserFailure_fallsBackToRuleBasedIntent() {
        FeedbackIntentParser failingPrimaryParser = feedback -> {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        };
        FallbackFeedbackIntentParser parser = new FallbackFeedbackIntentParser(
                Optional.of(failingPrimaryParser),
                new RuleBasedFeedbackIntentParser(),
                enabledProperties()
        );

        FeedbackIntent intent = parser.parse("수납 늘려줘");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.STORAGE_PRIORITY);
        assertThat(intent.interpretedIntent())
                .containsEntry("storagePriority", "HIGH")
                .containsEntry("source", "RULE_BASED")
                .containsEntry("fallbackUsed", true);
    }

    @Test
    void parse_withoutPrimaryParser_usesRuleBasedIntentOnly() {
        FallbackFeedbackIntentParser parser = new FallbackFeedbackIntentParser(
                Optional.empty(),
                new RuleBasedFeedbackIntentParser(),
                new LlmFeedbackProperties()
        );

        FeedbackIntent intent = parser.parse("방이 넓어 보이게");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.OPEN_SPACE_PRIORITY);
        assertThat(intent.interpretedIntent())
                .containsEntry("openSpacePriority", "HIGH")
                .doesNotContainKeys("source", "fallbackUsed");
    }

    private LlmFeedbackProperties enabledProperties() {
        LlmFeedbackProperties properties = new LlmFeedbackProperties();
        properties.setEnabled(true);
        return properties;
    }
}
