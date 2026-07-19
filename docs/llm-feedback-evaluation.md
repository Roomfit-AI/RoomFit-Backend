# LLM feedback evaluation

`./gradlew clean test` runs the deterministic semantic corpus without an API key or network access. It evaluates the final `FeedbackPlan`, fallback source, target/reference identity, operation order, metadata requirements, and executor safety rather than matching provider JSON text.

To run the small non-production live smoke corpus, an authorized operator must provide all four environment variables only in the process environment:

```bash
ROOMFIT_LLM_FEEDBACK_ENABLED=true \
ROOMFIT_LLM_API_KEY='provided-out-of-band' \
ROOMFIT_LLM_BASE_URL='https://approved-non-production-provider.example/v1' \
ROOMFIT_LLM_MODEL='approved-model' \
# Optional: 1-3 repeats; defaults to 1 to limit evaluation cost.
ROOMFIT_LLM_EVALUATION_RUNS=1 \
# Optional evaluation-only network timeout, 3,000-30,000 ms; defaults to 15,000 ms.
ROOMFIT_LLM_EVALUATION_TIMEOUT_MS=15000 \
./gradlew realLlmEvaluation
```

The task is skipped without the explicit configuration. A malformed URL, placeholder key, or blank model fails before an external request. It never runs as part of `test` or `build`. Do not put credentials in project files. The live task reports only aggregate counts, case IDs, the configured model name, and the stable `feedback-plan-v2` prompt identifier; it never prints keys, headers, URLs, layouts, IDs, coordinates, raw prompts, or raw provider responses.

When a provider result falls back, the summary aggregates a bounded `failureStages` map. Its values identify only `PROVIDER_CALL`, an HTTP status category, `RESPONSE_NOT_JSON_OBJECT`, `JSON_PARSE`, `PLAN_SCHEMA_OR_ENUM`, `SEMANTIC_VALIDATOR`, `CATALOG_METADATA_OR_SWAP_SAFETY`, `EXECUTION_SAFETY`, or `OTHER_SAFETY_POLICY`. The fallback result is never counted as LLM accuracy. If no provider plan is accepted, `llmOnlyAccuracy=N/A` and the evaluation fails.

An authorized non-production run must produce `unsafe=0`, `fallbacks=0`, and full semantic accuracy. A fallback during the live corpus is a quality failure rather than an approval to silently substitute rules for the provider.

Minimum review gates are: all deterministic tests pass, live semantic cases meet the above zero-fallback/zero-unsafe gate, expected clarification cases remain safe, deterministic malformed-output cases fall back safely, and a human reviews the provider/model approval separately from deployment.
