package com.roomfit.placement;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.llm.LlmClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmFeedbackIntentParserTest {

    @Test
    void parse_withEnlargeFurnitureDesk_returnsLargerDeskIntent() {
        LlmFeedbackIntentParser parser = parserReturning("""
                {
                  "intent": "ENLARGE_FURNITURE",
                  "targetFurniture": "desk",
                  "constraints": {
                    "minWidth": 1.4
                  },
                  "priority": "study_comfort"
                }
                """);

        FeedbackIntent intent = parser.parse("책상 더 크게");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.LARGER_DESK);
        assertThat(intent.interpretedIntent())
                .containsEntry("deskMinWidth", 1.4)
                .containsEntry("source", "LLM")
                .containsEntry("rawIntent", "ENLARGE_FURNITURE")
                .containsEntry("targetFurniture", "desk")
                .containsEntry("priority", "study_comfort")
                .containsEntry("fallbackUsed", false);
        assertThat(intent.interpretedIntent().get("constraints"))
                .isEqualTo(Map.of("minWidth", 1.4));
    }

    @Test
    void parse_withTooLargeMinWidth_clampsDeskMinWidth() {
        LlmFeedbackIntentParser parser = parserReturning("""
                {
                  "intent": "ENLARGE_FURNITURE",
                  "targetFurniture": "desk",
                  "constraints": {
                    "minWidth": 3.0
                  }
                }
                """);

        FeedbackIntent intent = parser.parse("책상 아주 크게");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.LARGER_DESK);
        assertThat(intent.interpretedIntent()).containsEntry("deskMinWidth", 1.8);
        assertThat(intent.interpretedIntent().get("constraints"))
                .isEqualTo(Map.of("minWidth", 1.8));
    }

    @Test
    void parse_withIncreaseStorage_returnsStoragePriorityIntent() {
        LlmFeedbackIntentParser parser = parserReturning("""
                {
                  "intent": "INCREASE_STORAGE",
                  "priority": "storage"
                }
                """);

        FeedbackIntent intent = parser.parse("수납을 더 늘리고 싶어");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.STORAGE_PRIORITY);
        assertThat(intent.interpretedIntent())
                .containsEntry("storagePriority", "HIGH")
                .containsEntry("source", "LLM")
                .containsEntry("rawIntent", "INCREASE_STORAGE")
                .containsEntry("fallbackUsed", false);
    }

    @Test
    void parse_withMakeRoomSpacious_returnsOpenSpacePriorityIntent() {
        LlmFeedbackIntentParser parser = parserReturning("""
                {
                  "intent": "MAKE_ROOM_SPACIOUS",
                  "priority": "open_space"
                }
                """);

        FeedbackIntent intent = parser.parse("방이 넓어 보였으면 좋겠어");

        assertThat(intent.type()).isEqualTo(FeedbackIntentType.OPEN_SPACE_PRIORITY);
        assertThat(intent.interpretedIntent())
                .containsEntry("openSpacePriority", "HIGH")
                .containsEntry("source", "LLM")
                .containsEntry("rawIntent", "MAKE_ROOM_SPACIOUS")
                .containsEntry("fallbackUsed", false);
    }

    @Test
    void parse_withUnsupportedIntent_throwsUnsupportedFeedbackIntent() {
        LlmFeedbackIntentParser parser = parserReturning("""
                {
                  "intent": "CHANGE_COLOR",
                  "targetFurniture": "bed"
                }
                """);

        assertCustomException(parser, ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
    }

    @Test
    void parse_withMalformedJson_throwsInvalidRequestBody() {
        LlmFeedbackIntentParser parser = parserReturning("{ malformed-json");

        assertCustomException(parser, ErrorCode.INVALID_REQUEST_BODY);
    }

    @Test
    void parse_ignoresCoordinateAndRotationFields() {
        LlmFeedbackIntentParser parser = parserReturning("""
                {
                  "intent": "ENLARGE_FURNITURE",
                  "targetFurniture": "desk",
                  "constraints": {
                    "minWidth": 1.4,
                    "x": 2.0,
                    "z": 1.0,
                    "rotation": 90
                  },
                  "x": 2.0,
                  "z": 1.0,
                  "rotation": 90
                }
                """);

        FeedbackIntent intent = parser.parse("책상을 키워줘");

        assertThat(intent.interpretedIntent()).doesNotContainKeys("x", "z", "rotation");
        assertThat(intent.interpretedIntent().get("constraints"))
                .isEqualTo(Map.of("minWidth", 1.4));
    }

    private LlmFeedbackIntentParser parserReturning(String response) {
        return new LlmFeedbackIntentParser(new FakeLlmClient(response));
    }

    private void assertCustomException(LlmFeedbackIntentParser parser, ErrorCode errorCode) {
        assertThatThrownBy(() -> parser.parse("피드백"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }

    private record FakeLlmClient(String response) implements LlmClient {

        @Override
        public String complete(String prompt) {
            assertThat(prompt).contains("JSON only");
            assertThat(prompt).contains("Do not generate x, z, rotation");
            return response;
        }
    }
}
