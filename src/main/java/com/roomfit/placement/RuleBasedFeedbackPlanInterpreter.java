package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.Furniture;
import com.roomfit.room.Room;

import java.util.List;

public class RuleBasedFeedbackPlanInterpreter implements FeedbackPlanInterpreter {

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context) {
        String normalized = feedback == null ? "" : feedback.trim();
        FeedbackOperation operation = switch (normalized) {
            case "책상 더 크게", "책상을 조금 더 넓게 쓰고 싶어", "책상을 넓게", "책상 크게", "책상 키워줘", "책상이 더 컸으면 좋겠어" ->
                    new FeedbackOperation(FeedbackOperationType.REPLACE_PRODUCT, null, null, null,
                            new FeedbackReplaceConstraints("desk", true, null, List.of(), List.of(), false));
            case "수납 늘려줘", "수납공간이 많은 책상으로 바꿔줘" ->
                    new FeedbackOperation(FeedbackOperationType.REPLACE_PRODUCT, null, null, null,
                            new FeedbackReplaceConstraints("desk", false, null, List.of(), List.of(), true));
            case "방이 넓어 보이게" ->
                    new FeedbackOperation(FeedbackOperationType.MOVE, FeedbackDirection.CENTER, 0.3, null, null);
            default -> throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        };
        Furniture desk = furniture.stream().filter(item -> "desk".equals(item.getType())).findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.FURNITURE_NOT_FOUND));
        return new FeedbackPlan("1.0", desk.getId(), "desk", List.of(operation), normalized,
                FeedbackSource.RULE_BASED, true);
    }
}
