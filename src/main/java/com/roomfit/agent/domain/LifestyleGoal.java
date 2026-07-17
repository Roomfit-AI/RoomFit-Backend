package com.roomfit.agent.domain;

public enum LifestyleGoal {
    STUDY_FOCUSED,
    RELAX_FOCUSED,
    STORAGE_FOCUSED,
    WFH_FOCUSED;

    // Furniture Variant Registry의 공식 lifestyleTags 어휘(WORK_STUDY/STORAGE/REST/
    // HOBBY_LEISURE)로의 명시적 정규화. HOBBY_LEISURE는 대응되는 LifestyleGoal이 아직
    // 없어 이 매핑에 없다 — 근거 없이 임의로 끼워 맞추지 않는다.
    public String toLifestyleTag() {
        return switch (this) {
            case STUDY_FOCUSED, WFH_FOCUSED -> "WORK_STUDY";
            case STORAGE_FOCUSED -> "STORAGE";
            case RELAX_FOCUSED -> "REST";
        };
    }
}
