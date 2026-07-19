package com.roomfit.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.product.repository.MockProductRepository;
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
    private final DeterministicFeedbackExecutor executor =
            new DeterministicFeedbackExecutor(new ValidationService(), new MockProductRepository());

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
    void parsesAddWithReferenceAndProductRequirements() {
        FeedbackPlan plan = interpret("침대 옆에 작은 협탁을 추가해줘", """
                {
                  "version":"2.0",
                  "requestKind":"DIRECT",
                  "operations":[{
                    "operationId":"op-1",
                    "type":"ADD_FURNITURE",
                    "target":{"furnitureType":"nightstand"},
                    "referenceTarget":{"furnitureType":"bed"},
                    "placement":{"relation":"NEXT_TO","side":"RIGHT"},
                    "productRequirements":{"furnitureType":"nightstand","sizePreference":"SMALL","storagePreferred":false,"styleKeywords":[]},
                    "dependsOn":[]
                  }],
                  "goals":[],"clarification":null,"reason":"add nightstand"
                }
                """);

        FeedbackOperation operation = plan.operations().getFirst();
        assertThat(operation.type()).isEqualTo(FeedbackOperationType.ADD_FURNITURE);
        assertThat(operation.referenceTarget().furnitureType()).isEqualTo("bed");
        assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.NEXT_TO);
        assertThat(operation.placement().side()).isEqualTo(FeedbackSide.RIGHT);
        assertThat(operation.productRequirements().sizePreference()).isEqualTo(FeedbackSizePreference.SMALL);
    }

    @Test
    void normalizesImplicitProviderAddToMoveForOneExistingFurniture() {
        Furniture chair = chair("chair-1", 2, 2);
        FeedbackPlan plan = implicitAddInterpreter().interpret("의자를 구석에 넣어줘",
                room(), List.of(chair), context());
        FeedbackExecution execution = executor.execute(plan, room(), List.of(chair));

        assertThat(plan.source()).isEqualTo(FeedbackSource.LLM);
        assertThat(plan.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.target().furnitureId()).isEqualTo("chair-1");
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.IN_CORNER);
        });
        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture()).hasSize(1);
        assertThat(execution.furniture().getFirst().getId()).isEqualTo("chair-1");
    }

    @Test
    void convertsImplicitProviderAddToClarificationWhenTargetIsAbsentOrAmbiguous() {
        LlmFeedbackPlanInterpreter interpreter = implicitAddInterpreter();
        Furniture first = chair("chair-1", 2, 2);
        Furniture second = chair("chair-2", 4, 4);

        FeedbackPlan absent = interpreter.interpret("의자를 구석에 넣어줘", room(), List.of(), context());
        FeedbackPlan ambiguous = interpreter.interpret("의자를 구석에 넣어줘", room(), List.of(first, second), context());

        assertThat(absent.needsClarification()).isTrue();
        assertThat(absent.operations()).isEmpty();
        assertThat(ambiguous.needsClarification()).isTrue();
        assertThat(ambiguous.operations()).isEmpty();
    }

    @Test
    void preservesExplicitAddButNeverTreatsImplicitWishesAsNewFurniture() {
        Furniture chair = chair("chair-1", 2, 2);
        Furniture sofa = new Furniture("sofa-1", "sofa", "기존 소파", 1.8, 0.8, 0.8,
                new Position(3, 3), 0, FurnitureStatus.EXISTING,
                "sofa-classic-ektorp-01", List.of("classic"), "sofa-classic-ektorp");

        FeedbackPlan wish = implicitAddInterpreter("desk_chair").interpret("의자가 구석에 있었으면 좋겠",
                room(), List.of(chair), context());
        FeedbackPlan sofaMove = implicitAddInterpreter("sofa").interpret("소파를 창가에 배치해줘",
                room(), List.of(sofa), context());
        FeedbackPlan existingExplicitAdd = implicitAddInterpreter("desk_chair").interpret("의자 하나 더 구석에 넣어줘",
                room(), List.of(chair), context());
        FeedbackPlan absentExplicitAdd = implicitAddInterpreter("desk_chair").interpret("의자 하나 추가해줘",
                room(), List.of(), context());

        assertThat(wish.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE));
        assertThat(sofaMove.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.NEAR_WINDOW);
        });
        assertThat(existingExplicitAdd.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.ADD_FURNITURE));
        assertThat(absentExplicitAdd.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.ADD_FURNITURE));
    }

    @Test
    void promptIncludesReferenceMoveContractAndSafeJsonShape() {
        String[] capturedPrompt = new String[1];
        Furniture monitor = monitor();
        Furniture desk = desk();
        LlmFeedbackPlanInterpreter interpreter = new LlmFeedbackPlanInterpreter(prompt -> {
            capturedPrompt[0] = prompt;
            return referenceMoveResponse("NEXT_TO", true);
        }, objectMapper);

        interpreter.interpret("모니터를 책상 가까이 옮겨줘", room(), List.of(monitor, desk), context());

        assertThat(capturedPrompt[0])
                .contains("A를 B 가까이/옆에/주변에 옮겨줘")
                .contains("모니터를 책상 가까이 옮겨줘")
                .contains("\"referenceTarget\":{\"furnitureType\":\"desk\"}")
                .contains("NEXT_TO")
                .contains("must not include referenceTarget")
                .doesNotContain("\"target\":{\"furnitureId\":\"monitor-");
    }

    @Test
    void validReferenceMovePlanParsesValidatesAndExecutes() {
        Furniture monitor = monitor();
        Furniture desk = desk();
        FeedbackPlan plan = new LlmFeedbackPlanInterpreter(prompt -> referenceMoveResponse("NEXT_TO", true), objectMapper)
                .interpret("모니터를 책상 가까이 옮겨줘", room(), List.of(monitor, desk), context());

        assertThat(plan.source()).isEqualTo(FeedbackSource.LLM);
        assertThat(plan.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.target().furnitureId()).isEqualTo("monitor-1");
            assertThat(operation.referenceTarget().furnitureId()).isEqualTo("desk-1");
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.NEXT_TO);
            assertThat(operation.placement().magnitude()).isNull();
        });

        FeedbackExecution execution = executor.execute(plan, room(), List.of(monitor, desk), context());

        assertThat(execution.result().applied()).as(execution.result().noChangeReason()).isTrue();
        assertThat(execution.furniture()).hasSize(2);
        assertThat(execution.furniture().getFirst().getId()).isEqualTo("monitor-1");
    }

    @Test
    void referenceTargetWithNonReferenceRelationIsDiagnosedAndFallsBack() {
        String invalidPlan = referenceMoveResponse("NEAR_WALL", true);

        assertThatThrownBy(() -> new LlmFeedbackPlanInterpreter(prompt -> invalidPlan, objectMapper)
                .interpret("모니터를 책상 가까이 옮겨줘", room(), List.of(monitor(), desk()), context()))
                .isInstanceOf(LlmProviderException.class)
                .satisfies(error -> assertThat(((LlmProviderException) error).stage())
                        .isEqualTo(LlmProviderException.SEMANTIC_REFERENCE_PRESENT_WITH_NON_REFERENCE_RELATION));

        FeedbackPlan plan = new FallbackFeedbackPlanInterpreter(
                Optional.of(new LlmFeedbackPlanInterpreter(prompt -> invalidPlan, objectMapper)),
                new RuleBasedFeedbackPlanInterpreter())
                .interpret("모니터를 책상 가까이 옮겨줘", room(), List.of(monitor(), desk()), context());

        assertThat(plan.source()).isEqualTo(FeedbackSource.RULE_BASED);
        assertThat(plan.fallbackUsed()).isTrue();
        assertThat(plan.operations()).singleElement().satisfies(operation ->
                assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.NEXT_TO));
    }

    @Test
    void referenceRelationWithoutReferenceTargetIsDiagnosedAndFallsBack() {
        String invalidPlan = referenceMoveResponse("NEXT_TO", false);

        assertThatThrownBy(() -> new LlmFeedbackPlanInterpreter(prompt -> invalidPlan, objectMapper)
                .interpret("모니터를 책상 가까이 옮겨줘", room(), List.of(monitor(), desk()), context()))
                .isInstanceOf(LlmProviderException.class)
                .satisfies(error -> assertThat(((LlmProviderException) error).stage())
                        .isEqualTo(LlmProviderException.SEMANTIC_REFERENCE_RELATION_WITHOUT_REFERENCE));

        FeedbackPlan plan = new FallbackFeedbackPlanInterpreter(
                Optional.of(new LlmFeedbackPlanInterpreter(prompt -> invalidPlan, objectMapper)),
                new RuleBasedFeedbackPlanInterpreter())
                .interpret("모니터를 책상 가까이 옮겨줘", room(), List.of(monitor(), desk()), context());

        assertThat(plan.source()).isEqualTo(FeedbackSource.RULE_BASED);
        assertThat(plan.fallbackUsed()).isTrue();
        assertThat(plan.operations()).singleElement().satisfies(operation ->
                assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.NEXT_TO));
    }

    @Test
    void parsesRemoveWithLocationHint() {
        FeedbackPlan plan = interpret("가운데 의자를 빼줘", """
                {"version":"2.0","requestKind":"DIRECT","operations":[{
                  "operationId":"op-1","type":"REMOVE_FURNITURE",
                  "target":{"furnitureType":"chair","locationHint":"CENTER"},"dependsOn":[]
                }],"goals":[],"clarification":null,"reason":"remove center chair"}
                """);

        assertThat(plan.operations().getFirst().target().locationHint()).isEqualTo(FeedbackLocationHint.CENTER);
        assertThat(plan.operations().getFirst().target().furnitureType()).isEqualTo("desk_chair");
    }

    @Test
    void rejectsCrossTypeSwapWithoutCatalogIdentifiers() {
        assertInvalid("""
                {"version":"2.0","requestKind":"DIRECT","operations":[{
                  "operationId":"op-1","type":"SWAP_FURNITURE",
                  "target":{"furnitureType":"bookshelf"},
                  "replacementRequirements":{"furnitureType":"hanger","sizePreference":"SIMILAR","styleKeywords":[]},
                  "dependsOn":[]
                }],"goals":[],"clarification":null,"reason":"swap furniture"}
                """);
    }

    @Test
    void normalizesLlmMetadataSwapToTheCatalogBackedKeywordInsteadOfTrustingProviderTags() {
        Furniture drawer = new Furniture("drawer-1", "drawer_chest", "수납장", 0.8, 0.4, 1.0,
                new Position(2, 2), 0, FurnitureStatus.EXISTING);
        LlmFeedbackPlanInterpreter interpreter = new LlmFeedbackPlanInterpreter(prompt -> """
                {"version":"2.0","requestKind":"DIRECT","operations":[{
                  "operationId":"op-1","type":"SWAP_FURNITURE",
                  "target":{"furnitureId":"drawer-1","furnitureType":"drawer_chest"},
                  "replacementRequirements":{"furnitureType":"drawer_chest","sizePreference":"ANY","styleKeywords":["invented_tag"]},
                  "dependsOn":[]
                }],"goals":[],"clarification":null,"reason":"wood swap"}
                """, objectMapper);

        FeedbackPlan plan = interpreter.interpret("수납장을 우드 톤으로 바꿔줘", room(), List.of(drawer), context());

        assertThat(plan.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.SWAP_FURNITURE);
            assertThat(operation.target().furnitureId()).isEqualTo("drawer-1");
            assertThat(operation.replacementRequirements().furnitureType()).isEqualTo("drawer_chest");
            assertThat(operation.replacementRequirements().styleKeywords()).containsExactly("wood");
        });
    }

    @Test
    void parsesMoveAndAddComposite() {
        FeedbackPlan plan = interpret("책상을 옮기고 조명을 구석에 추가해줘", """
                {"version":"2.0","requestKind":"COMPOSITE","operations":[
                  {"operationId":"op-1","type":"MOVE","target":{"furnitureType":"desk"},
                   "placement":{"relation":"RIGHT","magnitude":"SMALL"},"dependsOn":[]},
                  {"operationId":"op-2","type":"ADD_FURNITURE","target":{"furnitureType":"lamp"},
                   "placement":{"relation":"IN_CORNER"},
                   "productRequirements":{"furnitureType":"lamp","sizePreference":"ANY","styleKeywords":[]},
                   "dependsOn":["op-1"]}
                ],"goals":[],"clarification":null,"reason":"move and add"}
                """);

        assertThat(plan.operations()).extracting(FeedbackOperation::type)
                .containsExactly(FeedbackOperationType.MOVE, FeedbackOperationType.ADD_FURNITURE);
        assertThat(plan.operations().get(1).target().furnitureType()).isEqualTo("mood_lamp");
        assertThat(plan.operations().get(1).productRequirements().furnitureType()).isEqualTo("mood_lamp");
    }

    @Test
    void parsesRemoveAndAddComposite() {
        FeedbackPlan plan = interpret("의자를 빼고 조명을 추가해줘", """
                {"version":"2.0","requestKind":"COMPOSITE","operations":[
                  {"operationId":"op-1","type":"REMOVE_FURNITURE","target":{"furnitureType":"chair"},"dependsOn":[]},
                  {"operationId":"op-2","type":"ADD_FURNITURE","target":{"furnitureType":"lamp"},
                   "placement":{"relation":"NEAR_WALL"},
                   "productRequirements":{"furnitureType":"lamp","sizePreference":"ANY","styleKeywords":[]},
                   "dependsOn":["op-1"]}
                ],"goals":[],"clarification":null,"reason":"remove and add"}
                """);

        assertThat(plan.operations()).extracting(FeedbackOperation::type)
                .containsExactly(FeedbackOperationType.REMOVE_FURNITURE, FeedbackOperationType.ADD_FURNITURE);
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
            "productId", "variantId", "product_id", "variant_id", "distance_meters"
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
    void unsupportedMaterialOperationUsesRuleBasedFallbackWithoutSecondLlmCall() {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        LlmFeedbackPlanInterpreter primary = new LlmFeedbackPlanInterpreter(prompt -> {
            calls.incrementAndGet();
            return directOperation("""
                    "type":"CHANGE_MATERIAL"
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
    void excludesDeletedFurnitureFromTheLlmPrompt() {
        String[] capturedPrompt = new String[1];
        Furniture deleted = new Furniture("deleted-desk", "desk", "삭제된 책상", 1.2, 0.6, 0.73,
                new Position(2, 2), 0, FurnitureStatus.DELETED);
        LlmFeedbackPlanInterpreter interpreter = new LlmFeedbackPlanInterpreter(prompt -> {
            capturedPrompt[0] = prompt;
            return directOperation("""
                    "type":"MOVE",
                    "placement":{"relation":"RIGHT","magnitude":"SMALL"}
                    """);
        }, objectMapper);

        interpreter.interpret("책상을 오른쪽으로 옮겨줘", room(), List.of(desk(), deleted), context());

        assertThat(capturedPrompt[0]).contains("desk-1").doesNotContain("deleted-desk");
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

    @Test
    void acceptsJsonInsideAMarkdownFenceWithoutRelaxingPlanValidation() {
        FeedbackPlan plan = interpret("책상을 옮겨줘", "```json\n" + directOperation("""
                "type":"MOVE",
                "placement":{"relation":"RIGHT","magnitude":"SMALL"}
                """) + "\n```");

        assertThat(plan.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE));
    }

    @Test
    void acceptsShortExplanatoryTextAroundAJsonPlan() {
        FeedbackPlan plan = interpret("책상을 옮겨줘", "해석 결과입니다.\n" + directOperation("""
                "type":"MOVE",
                "placement":{"relation":"RIGHT","magnitude":"SMALL"}
                """) + "\n감사합니다.");

        assertThat(plan.operations()).hasSize(1);
    }

    @Test
    void rejectsFabricatedTargetIdAndUnknownCanonicalType() {
        assertInvalid(directOperation("""
                "type":"MOVE",
                "placement":{"relation":"RIGHT","magnitude":"SMALL"}
                """).replace("desk-1", "invented-id"));
        assertInvalid(directOperation("""
                "type":"MOVE",
                "target":{"furnitureId":"desk-1","furnitureType":"piano"},
                "placement":{"relation":"RIGHT","magnitude":"SMALL"}
                """));
    }

    @Test
    void malformedProviderOutputUsesExistingRuleBasedFallback() {
        FallbackFeedbackPlanInterpreter interpreter = new FallbackFeedbackPlanInterpreter(
                Optional.of(new LlmFeedbackPlanInterpreter(prompt -> "not json", objectMapper)),
                new RuleBasedFeedbackPlanInterpreter());

        FeedbackPlan plan = interpreter.interpret("책상 더 크게", room(), List.of(desk()), context());

        assertThat(plan.source()).isEqualTo(FeedbackSource.RULE_BASED);
        assertThat(plan.fallbackUsed()).isTrue();
    }

    private FeedbackPlan interpret(String feedback, String response) {
        return new LlmFeedbackPlanInterpreter(prompt -> response, objectMapper)
                .interpret(feedback, room(), List.of(desk()), context());
    }

    private LlmFeedbackPlanInterpreter implicitAddInterpreter() {
        return implicitAddInterpreter("desk_chair");
    }

    private LlmFeedbackPlanInterpreter implicitAddInterpreter(String furnitureType) {
        return new LlmFeedbackPlanInterpreter(prompt -> """
                {"version":"2.0","requestKind":"DIRECT","operations":[{
                  "operationId":"op-1","type":"ADD_FURNITURE",
                  "target":{"furnitureType":"%s"},
                  "placement":{"relation":"IN_CORNER"},
                  "productRequirements":{"furnitureType":"%s","sizePreference":"ANY","styleKeywords":[]},
                  "dependsOn":[]
                }],"goals":[],"clarification":null,"reason":"provider add"}
                """.formatted(furnitureType, furnitureType), objectMapper);
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

    private String referenceMoveResponse(String relation, boolean includeReferenceTarget) {
        String referenceTarget = includeReferenceTarget
                ? "\"referenceTarget\":{\"furnitureId\":\"desk-1\",\"furnitureType\":\"desk\"},"
                : "";
        String magnitude = "NEXT_TO".equals(relation) ? "" : ",\"magnitude\":\"MEDIUM\"";
        return """
                {"version":"2.0","requestKind":"DIRECT","operations":[{
                  "operationId":"op-1","type":"MOVE",
                  "target":{"furnitureId":"monitor-1","furnitureType":"monitor"},%s
                  "placement":{"relation":"%s"%s},"dependsOn":[]
                }],"goals":[],"clarification":null,"reason":"reference move"}
                """.formatted(referenceTarget, relation, magnitude);
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

    private Furniture monitor() {
        return new Furniture("monitor-1", "monitor", "기존 모니터", 0.5, 0.25, 0.4,
                new Position(1, 1), 0, FurnitureStatus.EXISTING,
                "monitor-basic-01", List.of("minimal"), "monitor-basic");
    }

    private Furniture chair(String id, double x, double z) {
        return new Furniture(id, "desk_chair", "기존 의자", 0.5, 0.5, 0.8,
                new Position(x, z), 0, FurnitureStatus.EXISTING,
                "desk-chair-basic-01", List.of("minimal"), "desk-chair-basic");
    }

    private AgentContext context() {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of("desk"), List.of(), List.of(1L), List.of("desk-compact-01"), List.of("minimal"));
    }
}
