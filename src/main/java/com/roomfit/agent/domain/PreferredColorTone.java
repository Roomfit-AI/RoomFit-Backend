package com.roomfit.agent.domain;

/**
 * 선호 색감 톤. DesignStyle의 WHITE_TONE/WOOD_TONE과 별개 축으로, Product 추천
 * 점수에는 아직 반영하지 않는다(Material Palette 매칭은 별도 후속 작업) — 저장/응답과
 * LLM 프롬프트 전달까지만 이번 범위.
 */
public enum PreferredColorTone {
    WHITE_IVORY,
    BEIGE_SAND,
    GRAY,
    BROWN_WOOD,
    GREEN_OLIVE,
    BLUE_NAVY,
    PINK_CORAL,
    BLACK_DARK
}
