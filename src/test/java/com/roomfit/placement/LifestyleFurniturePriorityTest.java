package com.roomfit.placement;

import com.roomfit.agent.domain.LifestyleGoal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LifestyleFurniturePriorityTest {

    @Test
    void rank_studyFocused_ranksDeskBeforeBookshelf() {
        int deskRank = LifestyleFurniturePriority.rank(LifestyleGoal.STUDY_FOCUSED, "desk");
        int bookshelfRank = LifestyleFurniturePriority.rank(LifestyleGoal.STUDY_FOCUSED, "bookshelf");

        assertThat(deskRank).isLessThan(bookshelfRank);
    }

    @Test
    void rank_wfhFocused_usesSameOrderAsStudyFocused() {
        assertThat(LifestyleFurniturePriority.rank(LifestyleGoal.WFH_FOCUSED, "desk"))
                .isEqualTo(LifestyleFurniturePriority.rank(LifestyleGoal.STUDY_FOCUSED, "desk"));
    }

    @Test
    void rank_relaxFocused_ranksBedBeforeRug() {
        int bedRank = LifestyleFurniturePriority.rank(LifestyleGoal.RELAX_FOCUSED, "bed");
        int rugRank = LifestyleFurniturePriority.rank(LifestyleGoal.RELAX_FOCUSED, "rug");

        assertThat(bedRank).isLessThan(rugRank);
    }

    @Test
    void rank_storageFocused_ranksWardrobeBeforePartitionShelf() {
        int wardrobeRank = LifestyleFurniturePriority.rank(LifestyleGoal.STORAGE_FOCUSED, "wardrobe");
        int partitionShelfRank = LifestyleFurniturePriority.rank(LifestyleGoal.STORAGE_FOCUSED, "partition_shelf");

        assertThat(wardrobeRank).isLessThan(partitionShelfRank);
    }

    @Test
    void rank_hobbyFocused_ranksTvBeforeMoodLamp() {
        int tvRank = LifestyleFurniturePriority.rank(LifestyleGoal.HOBBY_FOCUSED, "tv");
        int moodLampRank = LifestyleFurniturePriority.rank(LifestyleGoal.HOBBY_FOCUSED, "mood_lamp");

        assertThat(tvRank).isLessThan(moodLampRank);
    }

    @Test
    void rank_typeNotInPriorityList_ranksAfterAllListedTypes() {
        int plantRank = LifestyleFurniturePriority.rank(LifestyleGoal.STUDY_FOCUSED, "plant");
        int moodLampRank = LifestyleFurniturePriority.rank(LifestyleGoal.STUDY_FOCUSED, "mood_lamp");

        // mood_lamp는 STUDY_FOCUSED 목록의 마지막 항목이다 — 목록에 없는 plant는
        // 그보다도 뒤로 밀려야 한다.
        assertThat(plantRank).isGreaterThan(moodLampRank);
    }

    @Test
    void rank_nullLifestyleGoal_alwaysRanksLast() {
        assertThat(LifestyleFurniturePriority.rank(null, "desk")).isEqualTo(Integer.MAX_VALUE);
    }
}
