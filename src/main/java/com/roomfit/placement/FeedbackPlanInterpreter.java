package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.room.Furniture;
import com.roomfit.room.Room;

import java.util.List;

public interface FeedbackPlanInterpreter {

    FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context);

    /**
     * The optional UI selection is only a disambiguation hint. Implementations
     * must still reject an unknown or inactive id instead of selecting another
     * active furniture item.
     */
    default FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context,
                                   String selectedFurnitureId) {
        return interpret(feedback, room, furniture, context);
    }
}
