package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.room.Furniture;
import com.roomfit.room.Room;

import java.util.List;

public interface FeedbackPlanInterpreter {

    FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context);

    /**
     * A UI selection can disambiguate a target, but it never authorizes a
     * provider to substitute a different furniture id.
     */
    default FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context,
                                   String selectedFurnitureId) {
        return interpret(feedback, room, furniture, context);
    }
}
