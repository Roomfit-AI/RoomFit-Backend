package com.roomfit.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmFeedbackPlanInterpreterV2Test {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesDirectMoveWithSemanticPlacement() {
        FeedbackPlan plan = interpret("책상을 오른쪽으로 옮겨줘", directOperation("""
                "type":"MOVE",
                "placement":{"relation":"RIGHT","magnitude":"SMALL"}
                """));

        FeedbackOperation operation = plan.operations().getFirst();
        assertThat(plan.version()).isEqualTo("2.0");
        assertThat(plan.requestKind()).isEqualTo(FeedbackRequestKind.DIRECT);
        assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
        assertThat(operation.target().furnitureType()).isEqualTo("desk");
        assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.RIGHT);
        assertThat(operation.placement().magnitude()).isEqualTo(FeedbackMagnitude.SMALL);
    }

    @Test
    void parsesDirectRotateWithSemanticOrientation() {
        FeedbackPlan plan = interpret("책상을 반 바퀴 돌려줘", directOperation("""
                "type":"ROTATE",
                "placement":{"orientation":"HALF_TURN"}
                """));

        assertThat(plan.operations().getFirst().placement().orientation())
                .isEqualTo(FeedbackOrientation.HALF_TURN);
    }

    @Test
    void parsesDirectReplaceProductWithSupportedCatalogConstraints() {
        FeedbackPlan plan = interpret("수납 책상으로 바꿔줘", directOperation("""
                "type":"REPLACE_PRODUCT",
                "constraints":{"furnitureType":"desk","storagePreferred":true}
                """));

        FeedbackReplaceConstraints constraints = plan.operations().getFirst().constraints();
        assertThat(constraints.furnitureType()).isEqualTo("desk");
        assertThat(constraints.storagePreferred()).isTrue();
    }

    @Test
    void parsesMoveAndRotateCompositeWithValidatedDependency() {
        FeedbackPlan plan = interpret("책상을 오른쪽으로 옮기고 돌려줘", """
                {
                  "version":"2.0",
                  "requestKind":"COMPOSITE",
                  "operations":[
                    {
                      "operationId":"op-1",
                      "type":"MOVE",
                      "target":{"furnitureType":"desk"},
                      "placement":{"relation":"RIGHT","magnitude":"MEDIUM"},
                      "dependsOn":[]
                    },
                    {
                      "operationId":"op-2",
                      "type":"ROTATE",
                      "target":{"furnitureType":"desk"},
                      "placement":{"orientation":"QUARTER_TURN_CW"},
                      "dependsOn":["op-1"]
                    }
                  ],
                  "goals":[],
                  "clarification":null,
                  "reason":"move and rotate desk"
                }
                """);

        assertThat(plan.requestKind()).isEqualTo(FeedbackRequestKind.COMPOSITE);
        assertThat(plan.operations()).extracting(FeedbackOperation::operationId)
                .containsExactly("op-1", "op-2");
        assertThat(plan.operations().get(1).dependsOn()).containsExactly("op-1");
    }

    @Test
    void parsesClarificationWithoutExecutableOperations() {
        FeedbackPlan plan = interpret("저거 옮겨줘", """
                {
                  "version":"2.0",
                  "requestKind":"CLARIFICATION",
                  "operations":[],
                  "goals":[],
                  "clarification":{"question":"어떤 가구를 옮길까요?"},
                  "reason":"ambiguous target"
                }
                """);

        assertThat(plan.needsClarification()).isTrue();
        assertThat(plan.clarification().question()).isEqualTo("어떤 가구를 옮길까요?");
    }

    @Test
    void rejectsDuplicateOperationIds() {
        assertInvalid(compositeWithSecondOperation("op-1", "[]"));
    }

    @Test
    void rejectsMissingOrForwardDependency() {
        assertInvalid(compositeWithSecondOperation("op-2", "[\"missing-op\"]"));
    }

    @Test
    void rejectsSelfDependency() {
        assertInvalid(compositeWithSecondOperation("op-2", "[\"op-2\"]"));
    }

    @Test
    void rejectsDirectRequestWithMultipleOperations() {
        assertInvalid(compositeWithSecondOperation("op-2", "[\"op-1\"]")
                .replace("\"requestKind\":\"COMPOSITE\"", "\"requestKind\":\"DIRECT\""));
    }

    @Test
    void rejectsMoreThanFourOperations() {
        String operation = """
                {"operationId":"op-%d","type":"MOVE","target":{"furnitureType":"desk"},
                 "placement":{"relation":"RIGHT","magnitude":"SMALL"},"dependsOn":[]}
                """;
        String operations = String.join(",", java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToObj(index -> operation.formatted(index))
                .toList());
        assertInvalid("""
                {"version":"2.0","requestKind":"COMPOSITE","operations":[%s],
                 "goals":[],"clarification":null,"reason":"too many"}
                """.formatted(operations));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "x", "z", "coordinates", "position", "distanceMeters", "rotation",
            "rotationDegrees", "score", "validationResult", "weight", "objectiveWeight",
            "productId", "variantId"
    })
    void rejectsForbiddenProviderFields(String forbiddenField) {
        String response = directOperation("""
                "type":"MOVE",
                "placement":{"relation":"RIGHT","magnitude":"SMALL"},
                "%s":1
                """.formatted(forbiddenField));

        assertInvalid(response);
    }

    @Test
    void unsupportedOperationUsesRuleBasedFallbackWithoutSecondLlmCall() {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        LlmFeedbackPlanInterpreter primary = new LlmFeedbackPlanInterpreter(prompt -> {
            calls.incrementAndGet();
            return directOperation("""
                    "type":"ADD_FURNITURE"
                    """);
        }, objectMapper);
        FallbackFeedbackPlanInterpreter interpreter = new FallbackFeedbackPlanInterpreter(
                Optional.of(primary), new RuleBasedFeedbackPlanInterpreter());

        FeedbackPlan plan = interpreter.interpret("책상 더 크게", room(), List.of(desk()), context());

        assertThat(calls).hasValue(1);
        assertThat(plan.source()).isEqualTo(FeedbackSource.RULE_BASED);
        assertThat(plan.fallbackUsed()).isTrue();
    }

    @Test
    void ruleBasedFallbackDoesNotChooseTheFirstOfMultipleDesks() {
        Furniture secondDesk = new Furniture("desk-2", "desk", "두 번째 책상", 1.4, 0.7, 0.73,
                new Position(4, 4), 0, FurnitureStatus.EXISTING);

        FeedbackPlan plan = new RuleBasedFeedbackPlanInterpreter().interpret(
                "책상 더 크게", room(), List.of(desk(), secondDesk), context());

        assertThat(plan.requestKind()).isEqualTo(FeedbackRequestKind.CLARIFICATION);
        assertThat(plan.operations()).isEmpty();
        assertThat(plan.clarification().question()).contains("어떤 책상");
    }

    private FeedbackPlan interpret(String feedback, String response) {
        return new LlmFeedbackPlanInterpreter(prompt -> response, objectMapper)
                .interpret(feedback, room(), List.of(desk()), context());
    }

    private void assertInvalid(String response) {
        assertThatThrownBy(() -> interpret("책상을 옮겨줘", response))
                .isInstanceOf(LlmProviderException.class);
    }

    private String directOperation(String operationFields) {
        return """
                {
                  "version":"2.0",
                  "requestKind":"DIRECT",
                  "operations":[{
                    "operationId":"op-1",
                    "target":{"furnitureId":"desk-1","furnitureType":"desk"},
                    "dependsOn":[],
                    %s
                  }],
                  "goals":[],
                  "clarification":null,
                  "reason":"test"
                }
                """.formatted(operationFields);
    }

    private String compositeWithSecondOperation(String secondId, String dependencyJson) {
        return """
                {
                  "version":"2.0",
                  "requestKind":"COMPOSITE",
                  "operations":[
                    {"operationId":"op-1","type":"MOVE","target":{"furnitureType":"desk"},
                     "placement":{"relation":"RIGHT","magnitude":"SMALL"},"dependsOn":[]},
                    {"operationId":"%s","type":"ROTATE","target":{"furnitureType":"desk"},
                     "placement":{"orientation":"HALF_TURN"},"dependsOn":%s}
                  ],
                  "goals":[],
                  "clarification":null,
                  "reason":"test"
                }
                """.formatted(secondId, dependencyJson);
    }

    private Room room() {
        return new Room(null, 6, 6, 2.4, "meter", List.of(), List.of());
    }

    private Furniture desk() {
        return new Furniture("desk-1", "desk", "컴팩트 책상", 1.2, 0.6, 0.73,
                new Position(2, 2), 0, FurnitureStatus.RECOMMENDED,
                "desk-compact-01", List.of("minimal"), "desk-compact");
    }

    private AgentContext context() {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L), List.of("desk-compact-01"), List.of("minimal"));
    }
}
