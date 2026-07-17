package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.room.Furniture;
import com.roomfit.room.Room;

import java.util.List;
import java.util.Optional;

public class FallbackFeedbackPlanInterpreter implements FeedbackPlanInterpreter {

    private final Optional<FeedbackPlanInterpreter> primary;
    private final FeedbackPlanInterpreter fallback;

    public FallbackFeedbackPlanInterpreter(Optional<FeedbackPlanInterpreter> primary, FeedbackPlanInterpreter fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context) {
        if (primary.isEmpty()) {
            return fallback.interpret(feedback, room, furniture, context);
        }
        try {
            return primary.get().interpret(feedback, room, furniture, context);
        } catch (LlmProviderException e) {
            return fallback.interpret(feedback, room, furniture, context);
        }
    }
}
