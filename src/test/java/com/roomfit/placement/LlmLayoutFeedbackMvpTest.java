package com.roomfit.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.llm.LlmClient;
import com.roomfit.product.repository.MockProductRepository;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Opening;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LlmLayoutFeedbackMvpTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeterministicFeedbackExecutor executor =
            new DeterministicFeedbackExecutor(new ValidationService(), new MockProductRepository());

    @Test
    void fakeProvider_isCalledAndMoveChangesPositionWithoutChangingProductLifecycle() {
        AtomicInteger calls = new AtomicInteger();
        LlmFeedbackPlanInterpreter interpreter = interpreter(prompt -> {
            calls.incrementAndGet();
            assertThat(prompt).contains("Do not produce coordinates or product IDs");
            return planJson("desk-1", "desk", """
                    {"type":"MOVE","direction":"RIGHT","distanceMeters":0.3}
                    """);
        });
        Furniture before = compactDesk();

        FeedbackPlan plan = interpreter.interpret("책상을 오른쪽으로 30cm 옮겨줘", room(6, 6), List.of(before), context());
        FeedbackExecution execution = executor.execute(plan, room(6, 6), List.of(before));
        Furniture after = execution.furniture().getFirst();

        assertThat(calls).hasValue(1);
        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.result().source()).isEqualTo(FeedbackSource.LLM);
        assertThat(execution.result().fallbackUsed()).isFalse();
        assertThat(after.getPosition().getX()).isEqualTo(2.3);
        assertThat(after.getPosition().getZ()).isEqualTo(before.getPosition().getZ());
        assertThat(after.getProductId()).isEqualTo(before.getProductId());
        assertThat(after.getVariantId()).isEqualTo(before.getVariantId());
        assertValid(room(6, 6), execution.furniture());
    }

    @Test
    void rotateChangesFootprintRotationWithoutChangingProductLifecycle() {
        Furniture before = compactDesk();
        FeedbackExecution execution = executor.execute(plan("desk-1", "desk", new FeedbackOperation(
                FeedbackOperationType.ROTATE, null, null, 90, null)), room(6, 6), List.of(before));
        Furniture after = execution.furniture().getFirst();

        assertThat(execution.result().applied()).isTrue();
        assertThat(after.getRotation()).isEqualTo(90);
        assertThat(after.getProductId()).isEqualTo(before.getProductId());
        assertThat(after.getVariantId()).isEqualTo(before.getVariantId());
        assertValid(room(6, 6), execution.furniture());
    }

    @Test
    void widerDeskReplacesProductAndNeverReselectsCurrentProduct() {
        Furniture before = compactDesk();
        FeedbackExecution execution = executor.execute(plan("desk-1", "desk", widerDesk()), room(6, 6), List.of(before));
        Furniture after = execution.furniture().getFirst();

        assertThat(execution.result().applied()).isTrue();
        assertThat(after.getWidth()).isGreaterThan(before.getWidth());
        assertThat(after.getProductId()).isNotEqualTo(before.getProductId());
        assertThat(after.getVariantId()).isNotEqualTo(before.getVariantId());
        assertValid(room(6, 6), execution.furniture());
    }

    @Test
    void widestDeskReturnsExplicitNoLargerProductNoOp() {
        Furniture widest = new Furniture("desk-1", "desk", "미드센추리 글라스 책상", 1.75, 0.74, 0.812,
                new Position(2.0, 2.0), 0, FurnitureStatus.RECOMMENDED,
                "desk-midcentury-glass-01", List.of("midcentury", "modern"), "desk-midcentury-glass");

        FeedbackExecution execution = executor.execute(plan("desk-1", "desk", widerDesk()), room(6, 6), List.of(widest));

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("NO_LARGER_PRODUCT_AVAILABLE");
        assertThat(execution.furniture()).containsExactly(widest);
    }

    @Test
    void storageReplacementUsesDedicatedStorageDeskOnly() {
        FeedbackExecution execution = executor.execute(plan("desk-1", "desk", storageDesk()), room(6, 6), List.of(compactDesk()));
        Furniture after = execution.furniture().getFirst();

        assertThat(execution.result().applied()).isTrue();
        assertThat(after.getProductId()).isEqualTo("desk-storage-01");
        assertThat(after.getVariantId()).isEqualTo("desk-storage");
        assertValid(room(6, 6), execution.furniture());
    }

    @Test
    void storageReplacementNeverFallsBackToGeneralDeskWhenNoValidPlacementExists() {
        Room blockedRoom = roomWithDoorCoveringTheFloor();
        FeedbackExecution execution = executor.execute(plan("desk-1", "desk", storageDesk()), blockedRoom, List.of(compactDesk()));

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("NO_VALID_PRODUCT_PLACEMENT");
        assertThat(execution.result().summary()).isEqualTo("수납형 책상을 배치할 수 있는 유효한 위치를 찾지 못했습니다.");
        assertThat(execution.furniture().getFirst().getProductId()).isEqualTo("desk-compact-01");
    }

    @Test
    void malformedProviderResponseFallsBackButUnknownOperationAndMissingTargetDoNotInventAnOperation() {
        FallbackFeedbackPlanInterpreter fallback = new FallbackFeedbackPlanInterpreter(
                Optional.of(interpreter(prompt -> "{ malformed")), new RuleBasedFeedbackPlanInterpreter());
        FeedbackPlan fallbackPlan = fallback.interpret("책상 더 크게", room(6, 6), List.of(compactDesk()), context());
        assertThat(fallbackPlan.source()).isEqualTo(FeedbackSource.RULE_BASED);
        assertThat(fallbackPlan.fallbackUsed()).isTrue();

        FeedbackPlan unsupported = interpreter(prompt -> planJson("desk-1", "desk", """
                {"type":"WARP_FURNITURE"}
                """)).interpret("가구를 추가해줘", room(6, 6), List.of(compactDesk()), context());
        FeedbackExecution unsupportedExecution = executor.execute(unsupported, room(6, 6), List.of(compactDesk()));
        assertThat(unsupportedExecution.result().applied()).isFalse();
        assertThat(unsupportedExecution.result().noChangeReason()).isEqualTo("UNSUPPORTED_OPERATION");

        FeedbackPlan clarification = interpreter(prompt -> """
                {"version":"1.0","target":{"furnitureId":"","furnitureType":""},"operations":[],"reason":"target is ambiguous"}
                """).interpret("옮겨줘", room(6, 6), List.of(compactDesk()), context());
        FeedbackExecution clarificationExecution = executor.execute(clarification, room(6, 6), List.of(compactDesk()));
        assertThat(clarificationExecution.result().applied()).isFalse();
        assertThat(clarificationExecution.result().noChangeReason()).isEqualTo("NEEDS_CLARIFICATION");
    }

    @Test
    void providerFailureFallsBackToRuleBasedPlan() {
        FallbackFeedbackPlanInterpreter fallback = new FallbackFeedbackPlanInterpreter(
                Optional.of(interpreter(prompt -> { throw new IllegalStateException("provider timeout"); })),
                new RuleBasedFeedbackPlanInterpreter());

        FeedbackPlan plan = fallback.interpret("수납 늘려줘", room(6, 6), List.of(compactDesk()), context());

        assertThat(plan.source()).isEqualTo(FeedbackSource.RULE_BASED);
        assertThat(plan.fallbackUsed()).isTrue();
        assertThat(plan.operations()).extracting(operation -> operation.type())
                .containsExactly(FeedbackOperationType.REPLACE_PRODUCT);
    }

    private LlmFeedbackPlanInterpreter interpreter(LlmClient client) {
        return new LlmFeedbackPlanInterpreter(client, objectMapper);
    }

    private FeedbackPlan plan(String furnitureId, String furnitureType, FeedbackOperation operation) {
        return new FeedbackPlan("1.0", furnitureId, furnitureType, List.of(operation), "test", FeedbackSource.LLM, false);
    }

    private FeedbackOperation widerDesk() {
        return new FeedbackOperation(FeedbackOperationType.REPLACE_PRODUCT, null, null, null,
                new FeedbackReplaceConstraints("desk", true, null, List.of(), List.of(), false));
    }

    private FeedbackOperation storageDesk() {
        return new FeedbackOperation(FeedbackOperationType.REPLACE_PRODUCT, null, null, null,
                new FeedbackReplaceConstraints("desk", false, null, List.of(), List.of(), true));
    }

    private Furniture compactDesk() {
        return new Furniture("desk-1", "desk", "컴팩트 책상", 1.2, 0.6, 0.73,
                new Position(2.0, 2.0), 0, FurnitureStatus.RECOMMENDED,
                "desk-compact-01", List.of("minimal", "classic"), "desk-compact");
    }

    private Room room(double width, double depth) {
        return new Room(null, width, depth, 2.4, "meter", List.of(), List.of());
    }

    private Room roomWithDoorCoveringTheFloor() {
        return new Room(null, 2.0, 0.7, 2.4, "meter",
                List.of(new Opening("door-1", "door", "south", 0, 2.0, 2.0, null)), List.of());
    }

    private AgentContext context() {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L), List.of("desk-compact-01"), List.of("minimal"));
    }

    private void assertValid(Room room, List<Furniture> furniture) {
        ValidationResult result = new ValidationService().validate(room, furniture);
        assertThat(result.isCollisionFree()).isTrue();
        assertThat(result.isBoundaryValid()).isTrue();
        assertThat(result.isDoorClearance()).isTrue();
        assertThat(result.isWindowClearance()).isTrue();
        assertThat(result.isPathSecured()).isTrue();
    }

    private String planJson(String furnitureId, String furnitureType, String operation) {
        return """
                {"version":"1.0","target":{"furnitureId":"%s","furnitureType":"%s"},"operations":[%s],"reason":"test"}
                """.formatted(furnitureId, furnitureType, operation);
    }
}
