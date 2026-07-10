package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBasedFeedbackIntentParserTest {

    private final RuleBasedFeedbackIntentParser parser = new RuleBasedFeedbackIntentParser();

    @Test
    void parse_withLargerDesk_returnsLargerDeskIntent() {
        FeedbackIntent intent = parser.parse("책상 더 크게");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.LARGER_DESK);
        assertThat(intent.interpretedIntent()).containsEntry("deskMinWidth", 1.4);
    }

    @Test
    void parse_withStoragePriority_returnsStoragePriorityIntent() {
        FeedbackIntent intent = parser.parse("수납 늘려줘");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.STORAGE_PRIORITY);
        assertThat(intent.interpretedIntent()).containsEntry("storagePriority", "HIGH");
    }

    @Test
    void parse_withOpenSpacePriority_returnsOpenSpacePriorityIntent() {
        FeedbackIntent intent = parser.parse("방이 넓어 보이게");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.OPEN_SPACE_PRIORITY);
        assertThat(intent.interpretedIntent()).containsEntry("openSpacePriority", "HIGH");
    }

    @Test
    void parse_withUnsupportedSentence_throwsUnsupportedFeedbackIntent() {
        assertUnsupportedFeedbackIntent("침대 색 바꿔줘");
    }

    @Test
    void parse_withNullFeedback_throwsUnsupportedFeedbackIntent() {
        assertUnsupportedFeedbackIntent(null);
    }

    @Test
    void parse_withBlankFeedback_throwsUnsupportedFeedbackIntent() {
        assertUnsupportedFeedbackIntent("   ");
    }

    private void assertUnsupportedFeedbackIntent(String feedback) {
        assertThatThrownBy(() -> parser.parse(feedback))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
    }
}
