package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.config.LlmFeedbackProperties;
import com.roomfit.room.Room;

import java.util.Optional;

/**
 * FallbackFeedbackIntentParser와 동일한 패턴: LLM 구현체가 있으면 우선 시도하고,
 * 없거나 예외(호출 실패, JSON 파싱 실패, ValidationService 재검증 실패 등)가
 * 나면 RuleBasedPlacementService로 조용히 대체한다 — LayoutService.recommend()의
 * 기존 TODO("AI Agent 호출 실패 시 규칙 기반 fallback")를 충족.
 */
public class FallbackPlacementService implements PlacementService {

    private final Optional<PlacementService> primaryService;
    private final PlacementService ruleBasedService;
    private final LlmFeedbackProperties properties;

    public FallbackPlacementService(Optional<PlacementService> primaryService,
                                     PlacementService ruleBasedService,
                                     LlmFeedbackProperties properties) {
        this.primaryService = primaryService;
        this.ruleBasedService = ruleBasedService;
        this.properties = properties;
    }

    @Override
    public PlacementResult recommend(AgentContext context, Room room) {
        if (primaryService.isEmpty()) {
            return ruleBasedService.recommend(context, room);
        }

        try {
            return primaryService.get().recommend(context, room);
        } catch (RuntimeException ignored) {
            PlacementResult fallback = ruleBasedService.recommend(context, room);
            return new PlacementResult(RecommendationStatus.FALLBACK,
                    fallback.getRecommendedFurniture(), fallback.getScoreSummary());
        }
    }

    public boolean isLlmEnabled() {
        return properties.getPlacement().isEnabled();
    }

    public boolean hasPrimaryService() {
        return primaryService.isPresent();
    }
}
