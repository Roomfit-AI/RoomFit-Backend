package com.roomfit.placement;

import com.roomfit.config.LlmFeedbackProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "roomfit.llm.feedback.enabled=true")
class FeedbackIntentParserMissingLlmConfigTest {

    @Autowired
    private FeedbackIntentParser feedbackIntentParser;

    @Autowired
    private ObjectProvider<FeedbackIntentParser> feedbackIntentParsers;

    @Autowired
    private LlmFeedbackProperties properties;

    @Test
    void feedbackIntentParserBean_withEnabledButMissingClientConfig_usesRuleBasedFallbackOnly() {
        assertThat(feedbackIntentParsers.stream().toList()).hasSize(1);
        assertThat(properties.isEnabled()).isTrue();
        assertThat(feedbackIntentParser).isInstanceOf(FallbackFeedbackIntentParser.class);
        FallbackFeedbackIntentParser fallbackParser = (FallbackFeedbackIntentParser) feedbackIntentParser;
        assertThat(fallbackParser.hasPrimaryParser()).isFalse();

        FeedbackIntent intent = feedbackIntentParser.parse("책상 더 크게");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.LARGER_DESK);
        assertThat(intent.interpretedIntent())
                .containsEntry("deskMinWidth", 1.4)
                .containsEntry("source", "RULE_BASED")
                .containsEntry("rawIntent", "LARGER_DESK")
                .containsEntry("targetFurniture", "desk")
                .containsEntry("fallbackUsed", true);
    }
}
