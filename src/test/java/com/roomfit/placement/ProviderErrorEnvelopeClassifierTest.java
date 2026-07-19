package com.roomfit.placement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderErrorEnvelopeClassifierTest {

    @Test
    void summarizesCodeAndStatusWithoutMessage() {
        String summary = ProviderErrorEnvelopeClassifier.classify("""
                {"error":{"code":400,"status":"INVALID_ARGUMENT","message":"private provider detail"}}
                """);

        assertThat(summary).isEqualTo("JSON_ERROR_ENVELOPE_CODE_400_STATUS_INVALID_ARGUMENT");
        assertThat(summary).doesNotContain("private", "detail");
    }

    @Test
    void summarizesTypeAndAllowedParamOnly() {
        String summary = ProviderErrorEnvelopeClassifier.classify("""
                {"error":{"type":"invalid_request","param":"response_format","message":"opaque"}}
                """);

        assertThat(summary).isEqualTo("JSON_ERROR_ENVELOPE_TYPE_INVALID_REQUEST_FIELD_RESPONSE_FORMAT");
    }

    @Test
    void summarizesOnlyAllowedFieldViolations() {
        String summary = ProviderErrorEnvelopeClassifier.classify("""
                {"error":{"details":[{"fieldViolations":[
                  {"field":"messages"},{"field":"internal_secret_field"}
                ]}]}}
                """);

        assertThat(summary).isEqualTo("JSON_ERROR_ENVELOPE_FIELD_MESSAGES");
        assertThat(summary).doesNotContain("secret", "internal");
    }

    @Test
    void identifiesNonJsonAndUnrecognizedBodiesWithoutEchoingThem() {
        assertThat(ProviderErrorEnvelopeClassifier.classify("gateway diagnostic text"))
                .isEqualTo("NON_JSON_ERROR_BODY");
        assertThat(ProviderErrorEnvelopeClassifier.classify("{\"problem\":\"private detail\"}"))
                .isEqualTo("UNRECOGNIZED_ERROR_ENVELOPE");
    }
}
