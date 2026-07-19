package com.roomfit.placement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackVocabularyNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "침대,bed", "책꽂이,bookshelf", "커튼,curtain_blind", "데스크,desk",
            "사무용 의자,desk_chair", "전신거울,full_length_mirror", "행거,hanger",
            "티비장,media_console", "모니터,monitor", "무드등,mood_lamp", "멀티테이블,multi_table",
            "침대 옆 탁자,nightstand", "파티션 선반,partition_shelf", "식물,plant", "카펫,rug",
            "사이드테이블,side_table", "소파,sofa", "소파베드,sofa_bed", "티비,tv", "옷장,wardrobe",
            "서랍장,drawer_chest"
    })
    void normalizesSupportedKoreanAliases(String alias, String canonicalType) {
        assertThat(FeedbackVocabularyNormalizer.normalizeCanonicalType(alias)).isEqualTo(canonicalType);
    }

    @ParameterizedTest
    @CsvSource({"테이블", "수납장", "피아노"})
    void doesNotInventACanonicalTypeForAmbiguousOrUnsupportedWords(String word) {
        assertThat(FeedbackVocabularyNormalizer.normalizeCanonicalType(word)).isBlank();
    }
}
