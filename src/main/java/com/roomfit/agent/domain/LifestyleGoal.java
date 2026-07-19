package com.roomfit.agent.domain;

public enum LifestyleGoal {
    STUDY_FOCUSED,
    RELAX_FOCUSED,
    STORAGE_FOCUSED,
    WFH_FOCUSED,
    HOBBY_FOCUSED;

    // Furniture Variant Registry의 공식 lifestyleTags 어휘(WORK_STUDY/STORAGE/REST/
    // HOBBY_LEISURE)로의 명시적 정규화. Catalog에는 이미 HOBBY_LEISURE 태그가 달린
    // Product가 있어(furniture-catalog.json), 이 매핑만 없으면 그 신호가 어떤
    // 사용자 입력으로도 닿지 않는다 — HOBBY_FOCUSED로 명시적으로 연결한다.
    public String toLifestyleTag() {
        return switch (this) {
            case STUDY_FOCUSED, WFH_FOCUSED -> "WORK_STUDY";
            case STORAGE_FOCUSED -> "STORAGE";
            case RELAX_FOCUSED -> "REST";
            case HOBBY_FOCUSED -> "HOBBY_LEISURE";
        };
    }
}
