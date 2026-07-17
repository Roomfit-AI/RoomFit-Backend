package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.room.Furniture;
import com.roomfit.room.Room;

import java.util.List;

public interface FeedbackPlanInterpreter {

    FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context);
}
