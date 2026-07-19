package com.roomfit.agent.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LifestyleGoalTest {

    @Test
    void toLifestyleTag_mapsStudyAndWfhToWorkStudy() {
        assertThat(LifestyleGoal.STUDY_FOCUSED.toLifestyleTag()).isEqualTo("WORK_STUDY");
        assertThat(LifestyleGoal.WFH_FOCUSED.toLifestyleTag()).isEqualTo("WORK_STUDY");
    }

    @Test
    void toLifestyleTag_mapsStorageFocusedToStorage() {
        assertThat(LifestyleGoal.STORAGE_FOCUSED.toLifestyleTag()).isEqualTo("STORAGE");
    }

    @Test
    void toLifestyleTag_mapsRelaxFocusedToRest() {
        assertThat(LifestyleGoal.RELAX_FOCUSED.toLifestyleTag()).isEqualTo("REST");
    }

    @Test
    void toLifestyleTag_mapsHobbyFocusedToHobbyLeisure() {
        assertThat(LifestyleGoal.HOBBY_FOCUSED.toLifestyleTag()).isEqualTo("HOBBY_LEISURE");
    }
}
