package com.roomfit.agent.domain;

import java.util.Set;

/**
 * 선호 색감 톤. DesignStyle의 WHITE_TONE/WOOD_TONE과 별개 축이다.
 *
 * Generated Catalog의 각 Product는 렌더링에 쓰는 material id 목록(예: "wood",
 * "woodDark", "paintedWhite", "metal", "chrome")을 갖고 있다 — 이 8개 톤 중
 * WHITE_IVORY/BROWN_WOOD/GRAY 세 톤만 그 material id 어휘와 명확히 대응된다.
 * 나머지 다섯 톤(BEIGE_SAND/GREEN_OLIVE/BLUE_NAVY/PINK_CORAL/BLACK_DARK)은 현재
 * material 어휘에 확실히 대응되는 값이 없어 임의로 끼워 맞추지 않는다 —
 * toMaterialTags()가 빈 Set을 돌려주면 그 톤은 Product 선택 점수에 아직 반영되지
 * 않는다는 뜻이다(저장/응답과 LLM 프롬프트 전달에는 계속 쓰인다).
 */
public enum PreferredColorTone {
    WHITE_IVORY,
    BEIGE_SAND,
    GRAY,
    BROWN_WOOD,
    GREEN_OLIVE,
    BLUE_NAVY,
    PINK_CORAL,
    BLACK_DARK;

    public Set<String> toMaterialTags() {
        return switch (this) {
            case BROWN_WOOD -> Set.of("wood", "woodDark", "woodLight");
            case WHITE_IVORY -> Set.of("paintedWhite");
            case GRAY -> Set.of("metal", "metalLight", "chrome");
            case BEIGE_SAND, GREEN_OLIVE, BLUE_NAVY, PINK_CORAL, BLACK_DARK -> Set.of();
        };
    }
}
