package com.roomfit.placement;

import com.roomfit.agent.domain.LifestyleGoal;

import java.util.List;
import java.util.Map;

/**
 * lifestyleGoal별로 "이 목적에 특히 중요한" canonical furniture type의 상대 순서를
 * 정의한다. 사용자가 고르지 않은 가구를 새로 추가하는 데는 쓰이지 않는다 — 오직
 * RuleBasedPlacementService가 이미 사용자의 requiredItems/optionalItems에 들어있는
 * 항목들을 어떤 순서로 배치 시도할지 정렬하는 데만 쓰인다. 방 공간이나 12개 제한
 * 때문에 전부 배치하지 못할 때, 이 lifestyle과 더 관련 있는 항목이 먼저 배치를
 * 시도하도록 하기 위함이다.
 *
 * 목록에 없는 canonical type은 rank()가 목록 길이를 반환해 "관련 있는 항목들보다는
 * 뒤, 서로간에는 원래 순서 유지"가 되게 한다(안정 정렬 전제).
 */
final class LifestyleFurniturePriority {

    private static final List<String> WORK_STUDY_PRIORITY =
            List.of("desk", "desk_chair", "monitor", "bookshelf", "mood_lamp");
    private static final List<String> REST_PRIORITY =
            List.of("bed", "sofa_bed", "sofa", "nightstand", "mood_lamp", "rug");
    private static final List<String> STORAGE_PRIORITY =
            List.of("wardrobe", "drawer_chest", "hanger", "bookshelf", "partition_shelf");
    private static final List<String> HOBBY_LEISURE_PRIORITY =
            List.of("tv", "media_console", "sofa", "multi_table", "mood_lamp");

    private static final Map<LifestyleGoal, List<String>> PRIORITY_BY_GOAL = Map.of(
            LifestyleGoal.STUDY_FOCUSED, WORK_STUDY_PRIORITY,
            LifestyleGoal.WFH_FOCUSED, WORK_STUDY_PRIORITY,
            LifestyleGoal.RELAX_FOCUSED, REST_PRIORITY,
            LifestyleGoal.STORAGE_FOCUSED, STORAGE_PRIORITY,
            LifestyleGoal.HOBBY_FOCUSED, HOBBY_LEISURE_PRIORITY
    );

    private LifestyleFurniturePriority() {
    }

    /**
     * 순위가 낮을수록(=작은 정수) 우선 배치 대상. lifestyleGoal이 없거나 이 canonical
     * type이 해당 목적의 우선순위 목록에 없으면 목록 길이를 돌려줘, 명시적으로
     * 우선순위가 매겨진 항목들보다 항상 뒤로 정렬되게 한다.
     */
    static int rank(LifestyleGoal lifestyleGoal, String canonicalType) {
        if (lifestyleGoal == null || canonicalType == null) {
            return Integer.MAX_VALUE;
        }
        List<String> priority = PRIORITY_BY_GOAL.getOrDefault(lifestyleGoal, List.of());
        int index = priority.indexOf(canonicalType);
        return index < 0 ? priority.size() : index;
    }
}
