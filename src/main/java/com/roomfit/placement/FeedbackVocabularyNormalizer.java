package com.roomfit.placement;

import com.roomfit.product.catalog.GeneratedFurnitureCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The feedback interpreters share one Korean vocabulary.  It deliberately
 * returns an empty value for broad words such as "테이블" and "수납장": neither
 * is one of the executable catalog types, so guessing would be unsafe.
 */
public final class FeedbackVocabularyNormalizer {

    private static final Set<String> CANONICAL_TYPES = Set.of(
            "bed", "bookshelf", "curtain_blind", "desk", "desk_chair", "drawer_chest",
            "full_length_mirror", "hanger", "media_console", "monitor", "mood_lamp",
            "multi_table", "nightstand", "partition_shelf", "plant", "rug", "side_table",
            "sofa", "sofa_bed", "tv", "wardrobe"
    );
    private static final Map<String, String> KOREAN_ALIASES = aliases();

    private FeedbackVocabularyNormalizer() {
    }

    public static String normalizeCanonicalType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        String alias = KOREAN_ALIASES.get(compact);
        if (alias != null) {
            return alias;
        }
        String catalogType = GeneratedFurnitureCatalog.get().normalizeType(value);
        return catalogType != null && CANONICAL_TYPES.contains(catalogType) ? catalogType : "";
    }

    public static List<Map.Entry<String, String>> aliasesByLength() {
        return KOREAN_ALIASES.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()))
                .toList();
    }

    public static boolean isAmbiguousFurnitureWord(String value) {
        if (value == null) {
            return false;
        }
        boolean genericTable = value.contains("테이블")
                && !value.contains("멀티테이블")
                && !value.contains("사이드테이블")
                && !value.contains("사이드 테이블");
        return genericTable || value.contains("수납장");
    }

    private static Map<String, String> aliases() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("침대", "bed");
        values.put("책장", "bookshelf");
        values.put("책꽂이", "bookshelf");
        values.put("커튼", "curtain_blind");
        values.put("블라인드", "curtain_blind");
        values.put("책상", "desk");
        values.put("데스크", "desk");
        values.put("책상_의자", "desk_chair");
        values.put("책상의자", "desk_chair");
        values.put("사무용_의자", "desk_chair");
        values.put("의자", "desk_chair");
        values.put("서랍장", "drawer_chest");
        values.put("전신_거울", "full_length_mirror");
        values.put("전신거울", "full_length_mirror");
        values.put("행거", "hanger");
        values.put("tv장", "media_console");
        values.put("티비장", "media_console");
        values.put("미디어장", "media_console");
        values.put("미디어_콘솔", "media_console");
        values.put("미디어콘솔", "media_console");
        values.put("모니터", "monitor");
        values.put("무드등", "mood_lamp");
        values.put("스탠드_조명", "mood_lamp");
        values.put("조명", "mood_lamp");
        values.put("램프", "mood_lamp");
        values.put("스탠드", "mood_lamp");
        values.put("멀티테이블", "multi_table");
        values.put("식탁", "multi_table");
        values.put("협탁", "nightstand");
        values.put("침대_옆_탁자", "nightstand");
        values.put("사이드_테이블", "side_table");
        values.put("사이드테이블", "side_table");
        values.put("파티션_선반", "partition_shelf");
        values.put("파티션", "partition_shelf");
        values.put("화분", "plant");
        values.put("식물", "plant");
        values.put("러그", "rug");
        values.put("카펫", "rug");
        values.put("소파베드", "sofa_bed");
        values.put("소파_베드", "sofa_bed");
        values.put("소파", "sofa");
        values.put("tv", "tv");
        values.put("티비", "tv");
        values.put("옷장", "wardrobe");
        return Map.copyOf(values);
    }
}
