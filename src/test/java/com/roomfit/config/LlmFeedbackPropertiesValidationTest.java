package com.roomfit.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmFeedbackPropertiesValidationTest {

    @Test
    void acceptsOnlyUsableHttpClientConfiguration() {
        LlmFeedbackProperties properties = configured("https://llm.example.test/v1", "non-secret-test-token", "test-model");

        assertThat(properties.hasValidClientConfig()).isTrue();
    }

    @Test
    void rejectsMissingPlaceholderOrMalformedConfigurationWithoutAttemptingAClient() {
        assertThat(configured("", "token", "model").hasValidClientConfig()).isFalse();
        assertThat(configured("ftp://llm.example.test", "token", "model").hasValidClientConfig()).isFalse();
        assertThat(configured("https://llm.example.test", "placeholder", "model").hasValidClientConfig()).isFalse();
        assertThat(configured("https://llm.example.test", "token", " ").hasValidClientConfig()).isFalse();
    }

    private LlmFeedbackProperties configured(String baseUrl, String apiKey, String model) {
        LlmFeedbackProperties properties = new LlmFeedbackProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiKey(apiKey);
        properties.setModel(model);
        return properties;
    }
}
