package com.roomfit.placement;

import com.roomfit.agent.AgentContext;
import com.roomfit.room.Room;

/**
 * 배치 추천 인터페이스.
 * 규칙 기반 구현체(RuleBasedPlacementService)와 추후 AI Agent 기반 구현체를
 * 동일한 시그니처로 교체할 수 있도록 분리.
 */
public interface PlacementService {

    PlacementResult recommend(AgentContext context, Room room);
}
