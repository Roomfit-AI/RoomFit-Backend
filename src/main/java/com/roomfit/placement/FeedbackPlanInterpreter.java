package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.room.Furniture;
import com.roomfit.room.Room;

import java.util.List;

public interface FeedbackPlanInterpreter {

    FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context);

    /**
     * The optional UI selection only resolves target ambiguity. Implementations
     * must validate that it identifies an active furniture item of the requested
     * canonical type. It must never replace a different provider target or
     * resolve reference/product ambiguity.
     */
    default FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context,
                                   String selectedFurnitureId) {
        return interpret(feedback, room, furniture, context);
    }
}
