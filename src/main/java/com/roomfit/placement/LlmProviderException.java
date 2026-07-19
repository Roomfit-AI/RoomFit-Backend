package com.roomfit.placement;

public class LlmProviderException extends RuntimeException {

    public static final String PROVIDER_CALL = "PROVIDER_CALL";
    public static final String RESPONSE_NOT_JSON_OBJECT = "RESPONSE_NOT_JSON_OBJECT";
    public static final String JSON_PARSE = "JSON_PARSE";
    public static final String PLAN_SCHEMA_OR_ENUM = "PLAN_SCHEMA_OR_ENUM";
    public static final String SEMANTIC_VALIDATOR = "SEMANTIC_VALIDATOR";
    public static final String SEMANTIC_TARGET_REFERENCE = "SEMANTIC_TARGET_REFERENCE";
    public static final String SEMANTIC_TARGET_EMPTY = "SEMANTIC_TARGET_EMPTY";
    public static final String SEMANTIC_REFERENCE_EMPTY_OR_SAME = "SEMANTIC_REFERENCE_EMPTY_OR_SAME";
    public static final String SEMANTIC_REFERENCE_PRESENT_WITH_NON_REFERENCE_RELATION =
            "SEMANTIC_REFERENCE_PRESENT_WITH_NON_REFERENCE_RELATION";
    public static final String SEMANTIC_REFERENCE_RELATION_WITHOUT_REFERENCE =
            "SEMANTIC_REFERENCE_RELATION_WITHOUT_REFERENCE";
    public static final String SEMANTIC_TARGET_REFERENCE_UNRESOLVED = "SEMANTIC_TARGET_REFERENCE_UNRESOLVED";
    public static final String SEMANTIC_OPERATION_LIMIT = "SEMANTIC_OPERATION_LIMIT";
    public static final String SEMANTIC_OPERATION = "SEMANTIC_OPERATION";
    public static final String OTHER_SAFETY_POLICY = "OTHER_SAFETY_POLICY";

    private final String stage;

    public LlmProviderException(Throwable cause) {
        this(PROVIDER_CALL, cause);
    }

    public LlmProviderException(String stage, Throwable cause) {
        super(cause);
        this.stage = stage == null || stage.isBlank() ? PROVIDER_CALL : stage;
    }

    /** A bounded diagnostic category; it never contains provider response content or credentials. */
    public String stage() {
        return stage;
    }
}
