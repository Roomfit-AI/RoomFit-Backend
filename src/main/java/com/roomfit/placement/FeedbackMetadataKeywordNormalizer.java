package com.roomfit.placement;

import java.util.List;

/**
 * Maps only Korean terms whose backing values exist in the generated catalog's
 * materials field.  Unknown colour or temperature terms intentionally remain
 * unsupported instead of being guessed as a product style.
 */
final class FeedbackMetadataKeywordNormalizer {

    private static final List<String> WOOD_TERMS = List.of("우드", "나무", "목재", "원목");
    private static final List<String> LIGHT_TERMS = List.of("밝은 색", "밝은색", "밝은 톤", "화이트", "흰색");
    private static final List<String> METAL_TERMS = List.of("금속", "메탈", "철제");
    private static final List<String> UNSUPPORTED_TONE_TERMS = List.of(
            "다크", "어두운", "블랙", "검은", "따뜻한 톤", "차가운 톤", "다른 톤", "색상", "색 바");

    private FeedbackMetadataKeywordNormalizer() {
    }

    static List<String> keywordsFor(String feedback) {
        if (feedback == null) return List.of();
        if (containsAny(feedback, WOOD_TERMS)) return List.of("wood");
        if (containsAny(feedback, LIGHT_TERMS)) return List.of("paintedWhite");
        if (containsAny(feedback, METAL_TERMS)) return List.of("metal");
        return List.of();
    }

    static boolean containsMetadataRequest(String feedback) {
        return !keywordsFor(feedback).isEmpty() || containsAny(feedback, UNSUPPORTED_TONE_TERMS);
    }

    static List<String> supportedKeywords() {
        return List.of("wood", "paintedWhite", "metal");
    }

    private static boolean containsAny(String feedback, List<String> terms) {
        return terms.stream().anyMatch(feedback::contains);
    }
}
