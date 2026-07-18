package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Room;

import java.util.List;

public class RuleBasedFeedbackPlanInterpreter implements FeedbackPlanInterpreter {

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context) {
        String normalized = feedback == null ? "" : feedback.trim();
        if (!supports(normalized)) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        }
        List<Furniture> desks = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> "desk".equals(item.getType()))
                .toList();
        if (desks.isEmpty()) {
            throw new CustomException(ErrorCode.FURNITURE_NOT_FOUND);
        }
        if (desks.size() > 1) {
            return new FeedbackPlan("2.0", FeedbackRequestKind.CLARIFICATION, List.of(), List.of(),
                    new FeedbackClarification("어떤 책상을 변경할지 알려주세요."), normalized,
                    FeedbackSource.RULE_BASED, true);
        }
        Furniture desk = desks.getFirst();
        FeedbackTargetSelector target = new FeedbackTargetSelector(desk.getId(), "desk", "");
        FeedbackOperation operation = switch (normalized) {
            case "책상 더 크게", "책상을 조금 더 넓게 쓰고 싶어", "책상을 넓게", "책상 크게", "책상 키워줘", "책상이 더 컸으면 좋겠어" ->
                    new FeedbackOperation("op-1", FeedbackOperationType.REPLACE_PRODUCT, target, null,
                            new FeedbackReplaceConstraints("desk", true, null, List.of(), List.of(), false), List.of());
            case "수납 늘려줘", "수납공간이 많은 책상으로 바꿔줘" ->
                    new FeedbackOperation("op-1", FeedbackOperationType.REPLACE_PRODUCT, target, null,
                            new FeedbackReplaceConstraints("desk", false, null, List.of(), List.of(), true), List.of());
            case "방이 넓어 보이게" ->
                    new FeedbackOperation("op-1", FeedbackOperationType.MOVE, target,
                            new FeedbackPlacement(FeedbackRelation.CENTER, FeedbackMagnitude.MEDIUM, null), null, List.of());
            default -> throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        };
        return new FeedbackPlan("2.0", FeedbackRequestKind.DIRECT, List.of(operation), List.of(), null,
                normalized, FeedbackSource.RULE_BASED, true);
    }

    private boolean supports(String feedback) {
        return switch (feedback) {
            case "책상 더 크게", "책상을 조금 더 넓게 쓰고 싶어", "책상을 넓게", "책상 크게", "책상 키워줘",
                 "책상이 더 컸으면 좋겠어", "수납 늘려줘", "수납공간이 많은 책상으로 바꿔줘", "방이 넓어 보이게" -> true;
            default -> false;
        };
    }
}
