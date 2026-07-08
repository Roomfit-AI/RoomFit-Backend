package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 규칙 기반 배치 추천 구현체.
 *
 * TODO: 지금은 requiredItems를 단순 나열 배치하는 스켈레톤 수준.
 * 실제로는 lifestyleGoal/designStyle 별 배치 템플릿을 두고,
 * 방 크기 및 기존(existing) 가구와 겹치지 않는 템플릿을 선택하는 방식으로 구현 권장.
 * (본선에서 AI Agent 기반 구현체로 교체 시 PlacementService 인터페이스만 유지하면 됨)
 */
@Service
public class RuleBasedPlacementService implements PlacementService {

    @Override
    public PlacementResult recommend(AgentContext context, Room room) {
        List<Furniture> recommended = new ArrayList<>();

        double cursorX = 0.3;
        for (String itemType : context.getRequiredItems()) {
            // TODO: itemType 별 기본 크기 테이블 참조, 기존 가구와 겹치지 않는 위치 계산
            Furniture furniture = new Furniture(
                    itemType + "-rec-" + System.nanoTime(),
                    itemType,
                    itemType,
                    1.0, 0.6, 0.7,
                    new Position(cursorX, 0.3),
                    0,
                    FurnitureStatus.RECOMMENDED
            );
            recommended.add(furniture);
            cursorX += 1.2;
        }

        return new PlacementResult(RecommendationStatus.SUCCESS, recommended);
    }
}
