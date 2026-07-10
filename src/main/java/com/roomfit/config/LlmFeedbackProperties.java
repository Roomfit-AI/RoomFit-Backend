package com.roomfit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "roomfit.llm")
public class LlmFeedbackProperties {

    /**
     * LLM feedback parsing scaffold flag. The default stays disabled so the
     * current rule-based parser remains the only active behavior.
     */
    private final Feedback feedback = new Feedback();
    private String apiKey = "";
    private String baseUrl = "";
    private String model = "";
    private int timeoutMs = 3000;

    public boolean isEnabled() {
        return feedback.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.feedback.setEnabled(enabled);
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean hasValidClientConfig() {
        return hasText(apiKey) && hasText(baseUrl) && hasText(model);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static class Feedback {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
