package com.roomfit.agent.domain;

public enum DesignStyle {
    MINIMAL,
    NATURAL,
    WHITE_TONE,
    WOOD_TONE,
    COZY,
    MODERN,
    CLASSIC,
    MIDCENTURY;

    // MockProductRepository의 styleTags는 이 enum 값들과 동일한 소문자 어휘로
    // 등록되어 있다(예: MINIMAL -> "minimal", MIDCENTURY -> "midcentury") — 이 메서드가
    // 그 정규화를 한 곳에 명시해, 앞으로 값이 늘어나도 대소문자 변환 로직이
    // ProductRecommendationService 등 여러 곳에 흩어지지 않게 한다.
    public String toStyleTag() {
        return name().toLowerCase();
    }
}
