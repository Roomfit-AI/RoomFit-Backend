package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.config.LlmFeedbackProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "roomfit.llm.feedback.enabled=false",
        "roomfit.llm.api-key=",
        "roomfit.llm.base-url=",
        "roomfit.llm.model="
})
class FeedbackIntentParserBeanTest {

    @Autowired
    private FeedbackIntentParser feedbackIntentParser;

    @Autowired
    private ObjectProvider<FeedbackIntentParser> feedbackIntentParsers;

    @Autowired
    private LlmFeedbackProperties properties;

    @Test
    void feedbackIntentParserBean_isSingleFallbackParserWithLlmDisabledByDefault() {
        assertThat(feedbackIntentParsers.stream().toList()).hasSize(1);
        assertThat(feedbackIntentParser).isInstanceOf(FallbackFeedbackIntentParser.class);
        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void parse_withDefaultBean_returnsRuleBasedResults() {
        assertIntent(feedbackIntentParser.parse("책상 더 크게"),
                FeedbackIntentType.LARGER_DESK, Map.of("deskMinWidth", 1.4));
        assertIntent(feedbackIntentParser.parse("수납 늘려줘"),
                FeedbackIntentType.STORAGE_PRIORITY, Map.of("storagePriority", "HIGH"));
        assertIntent(feedbackIntentParser.parse("방이 넓어 보이게"),
                FeedbackIntentType.OPEN_SPACE_PRIORITY, Map.of("openSpacePriority", "HIGH"));
    }

    @Test
    void parse_withUnsupportedSentence_throwsUnsupportedFeedbackIntent() {
        assertThatThrownBy(() -> feedbackIntentParser.parse("침대 색 바꿔줘"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
    }

    private void assertIntent(FeedbackIntent intent, FeedbackIntentType type,
                              Map<String, Object> interpretedIntent) {
        assertThat(intent.type()).isEqualTo(type);
        assertThat(intent.interpretedIntent()).containsAllEntriesOf(interpretedIntent);
    }
}
