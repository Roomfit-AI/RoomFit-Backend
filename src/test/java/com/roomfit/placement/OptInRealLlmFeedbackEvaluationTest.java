package com.roomfit.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.config.LlmFeedbackProperties;
import com.roomfit.llm.OpenAiCompatibleLlmClient;
import com.roomfit.product.repository.MockProductRepository;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs only through the explicit realLlmEvaluation Gradle task; never through CI test/build. */
@Tag("llm-evaluation")
class OptInRealLlmFeedbackEvaluationTest {

    @Test
    void evaluatesSmallNonProductionSemanticCorpus() {
        LlmFeedbackProperties properties = propertiesFromEnvironment();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.hasValidClientConfig()).isTrue();

        ObjectMapper objectMapper = new ObjectMapper();
        LlmFeedbackPlanInterpreter primary = new LlmFeedbackPlanInterpreter(
                new OpenAiCompatibleLlmClient(properties, objectMapper), objectMapper);
        RuleBasedFeedbackPlanInterpreter fallback = new RuleBasedFeedbackPlanInterpreter();
        DeterministicFeedbackExecutor executor = new DeterministicFeedbackExecutor(
                new ValidationService(), new MockProductRepository());
        List<LiveCase> cases = List.of(
                new LiveCase("live-existing-move", "move", "의자를 구석에 배치해줘",
                        List.of(furniture("chair", "desk_chair", 4, 4)), null,
                        List.of(FeedbackOperationType.MOVE), List.of("desk_chair"), List.of("")),
                new LiveCase("live-reference-move", "reference-move", "모니터를 책상 가까이 옮겨줘",
                        List.of(furniture("monitor", "monitor", 2, 2), furniture("desk", "desk", 5, 5)), null,
                        List.of(FeedbackOperationType.MOVE), List.of("monitor"), List.of("desk")),
                new LiveCase("live-ambiguous-selection", "clarification", "우드 톤으로 바꿔줘",
                        List.of(furniture("chair", "desk_chair", 4, 4)), null, List.of(), List.of(), List.of())
        );
        int runs = evaluationRuns();

        int llmSemanticPassed = 0;
        int llmClarifications = 0;
        int fallbackClarifications = 0;
        int fallbacks = 0;
        int llmPlans = 0;
        int unsafe = 0;
        Map<String, Integer> failureStages = new TreeMap<>();
        StringBuilder failures = new StringBuilder();
        for (int run = 1; run <= runs; run++) {
            for (LiveCase evaluationCase : cases) {
                FeedbackPlan plan;
                boolean acceptedLlmPlan = false;
                String fallbackStage = "";
                try {
                    plan = primary.interpret(evaluationCase.feedback, room(), evaluationCase.furniture, context(),
                            evaluationCase.selectedFurnitureId);
                    acceptedLlmPlan = true;
                    llmPlans++;
                } catch (LlmProviderException failure) {
                    fallbackStage = failureStage(failure);
                    failureStages.merge(fallbackStage, 1, Integer::sum);
                    fallbacks++;
                    plan = fallback.interpret(evaluationCase.feedback, room(), evaluationCase.furniture, context(),
                            evaluationCase.selectedFurnitureId);
                }
                FeedbackExecution execution = executor.execute(plan, room(), evaluationCase.furniture, context());
                boolean expectedClarification = evaluationCase.operationTypes.isEmpty();
                boolean semanticMatch = expectedClarification ? plan.needsClarification()
                        : plan.operations().stream().map(FeedbackOperation::type).toList().equals(evaluationCase.operationTypes)
                        && plan.operations().stream().map(operation -> operation.target().furnitureType()).toList()
                        .equals(evaluationCase.targetTypes)
                        && plan.operations().stream().map(operation -> operation.referenceTarget() == null ? ""
                        : operation.referenceTarget().furnitureType()).toList().equals(evaluationCase.referenceTypes);
                boolean introducedAdd = plan.operations().stream().anyMatch(operation -> operation.type() == FeedbackOperationType.ADD_FURNITURE);
                boolean unsafeCase = introducedAdd || (expectedClarification && execution.result().applied());
                boolean executionMatch = expectedClarification ? !execution.result().applied() : execution.result().applied();
                if (acceptedLlmPlan && plan.needsClarification()) llmClarifications++;
                if (!acceptedLlmPlan && plan.needsClarification()) fallbackClarifications++;
                if (unsafeCase) unsafe++;
                if (acceptedLlmPlan && semanticMatch && executionMatch && !unsafeCase) {
                    llmSemanticPassed++;
                }
                if (acceptedLlmPlan && !executionMatch) {
                    String executionStage = plan.operations().stream()
                            .anyMatch(operation -> operation.type() == FeedbackOperationType.SWAP_FURNITURE)
                            ? "CATALOG_METADATA_OR_SWAP_SAFETY" : "EXECUTION_SAFETY";
                    failureStages.merge(executionStage, 1, Integer::sum);
                }
                if (!acceptedLlmPlan || !semanticMatch || !executionMatch || unsafeCase) {
                    if (!failures.isEmpty()) failures.append(", ");
                    failures.append(evaluationCase.id).append("@run-").append(run)
                            .append(":category=").append(evaluationCase.category)
                            .append(":expected=").append(expectedClarification ? "CLARIFICATION" : "EXECUTE")
                            .append(":actual=").append(acceptedLlmPlan
                                    ? semanticDescription(plan) + (executionMatch ? "" : "_EXECUTION_REJECTED")
                                    : "RULE_FALLBACK")
                            .append(":impact=").append(unsafeCase ? "UNSAFE" : "SAFE")
                            .append(acceptedLlmPlan ? "" : ":stage=" + fallbackStage);
                }
            }
        }

        int total = cases.size() * runs;
        String llmOnlyAccuracy = llmPlans == 0 ? "N/A" : (llmSemanticPassed * 100 / llmPlans) + "%";
        System.out.printf("LLM evaluation summary: model=%s prompt=feedback-plan-v2 runs=%d total=%d "
                        + "baseUrlProfile=%s timeoutMs=%d llmPlans=%d llmSemanticPassed=%d llmOnlyAccuracy=%s fallbacks=%d "
                        + "llmClarifications=%d fallbackClarifications=%d unsafe=%d failureStages=%s failed=%s%n",
                properties.getModel(), runs, total, baseUrlProfile(properties.getBaseUrl()), properties.getTimeoutMs(),
                llmPlans, llmSemanticPassed, llmOnlyAccuracy, fallbacks,
                llmClarifications, fallbackClarifications, unsafe, failureStages, failures.isEmpty() ? "none" : failures);
        assertThat(unsafe).isZero();
        assertThat(fallbacks).as("actual LLM evaluation must not pass through rule-based fallback").isZero();
        assertThat(llmPlans).isEqualTo(total);
        assertThat(llmSemanticPassed).isEqualTo(total);
    }

    private LlmFeedbackProperties propertiesFromEnvironment() {
        LlmFeedbackProperties properties = new LlmFeedbackProperties();
        properties.setEnabled("true".equalsIgnoreCase(System.getenv("ROOMFIT_LLM_FEEDBACK_ENABLED")));
        properties.setApiKey(System.getenv("ROOMFIT_LLM_API_KEY"));
        properties.setBaseUrl(System.getenv("ROOMFIT_LLM_BASE_URL"));
        properties.setModel(System.getenv("ROOMFIT_LLM_MODEL"));
        properties.setTimeoutMs(evaluationTimeoutMs());
        return properties;
    }

    private int evaluationRuns() {
        String configured = System.getenv("ROOMFIT_LLM_EVALUATION_RUNS");
        if (configured == null || configured.isBlank()) return 1;
        try {
            int runs = Integer.parseInt(configured.trim());
            return runs >= 1 && runs <= 3 ? runs : 1;
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    /** Evaluation-only timeout: production retains the application default. */
    private int evaluationTimeoutMs() {
        String configured = System.getenv("ROOMFIT_LLM_EVALUATION_TIMEOUT_MS");
        if (configured == null || configured.isBlank()) return 15_000;
        try {
            int timeoutMs = Integer.parseInt(configured.trim());
            return timeoutMs >= 3_000 && timeoutMs <= 30_000 ? timeoutMs : 15_000;
        } catch (NumberFormatException ignored) {
            return 15_000;
        }
    }

    private String failureStage(LlmProviderException failure) {
        Throwable cause = failure.getCause();
        if (LlmProviderException.PROVIDER_CALL.equals(failure.stage())
                && cause instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            if (status == 429) return "HTTP_STATUS_429_PROVIDER_RATE_LIMIT";
            if (status == 401 || status == 403) return "HTTP_STATUS_" + status + "_AUTH_OR_CREDENTIALS";
            if (status == 404) return "HTTP_STATUS_404_ENDPOINT_OR_MODEL";
            if (status == 400) return safeBadRequestStage(response);
            return "HTTP_STATUS_" + status;
        }
        if (LlmProviderException.PROVIDER_CALL.equals(failure.stage()) && cause != null) {
            return "PROVIDER_CALL_" + causeClassChain(cause);
        }
        return failure.stage();
    }

    /**
     * Reads a response body only in memory and emits a fixed, non-secret
     * category. It never logs or stores the provider's raw body or message.
     */
    private String safeBadRequestStage(RestClientResponseException response) {
        return "HTTP_STATUS_400_" + ProviderErrorEnvelopeClassifier.classify(response.getResponseBodyAsString());
    }

    private String causeClassChain(Throwable cause) {
        List<String> classes = new ArrayList<>();
        Throwable current = cause;
        while (current != null && classes.size() < 3) {
            classes.add(current.getClass().getSimpleName());
            current = current.getCause();
        }
        return String.join("_CAUSED_BY_", classes);
    }

    private String baseUrlProfile(String baseUrl) {
        try {
            String path = new URI(baseUrl == null ? "" : baseUrl.trim()).getPath();
            String normalized = path == null ? "" : path.toLowerCase(java.util.Locale.ROOT).replaceAll("/+$", "");
            if (normalized.endsWith("/chat/completions")) return "CHAT_COMPLETIONS_ENDPOINT";
            if (normalized.endsWith("/openai")) return "OPENAI_COMPATIBLE_ROOT";
            if (normalized.endsWith("/v1")) return "V1_ROOT";
            return "CUSTOM_HTTP_ROOT";
        } catch (URISyntaxException e) {
            return "INVALID_URL";
        }
    }

    private String semanticDescription(FeedbackPlan plan) {
        if (plan.needsClarification()) return "CLARIFICATION";
        return plan.operations().stream().map(operation -> operation.type().name()).collect(java.util.stream.Collectors.joining("+"));
    }

    private Furniture furniture(String id, String type, double x, double z) {
        return new Furniture(id, type, type, 0.6, 0.6, 0.8, new Position(x, z), 0, FurnitureStatus.EXISTING);
    }

    private Room room() {
        return new Room(null, 10, 10, 2.4, "meter", List.of(), List.of());
    }

    private AgentContext context() {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of(), List.of(), List.of(1L), List.of(), List.of("minimal"));
    }

    private record LiveCase(String id, String category, String feedback, List<Furniture> furniture,
                            String selectedFurnitureId, List<FeedbackOperationType> operationTypes,
                            List<String> targetTypes, List<String> referenceTypes) {
    }
}
