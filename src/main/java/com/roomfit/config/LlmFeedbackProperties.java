package com.roomfit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@ConfigurationProperties(prefix = "roomfit.llm")
public class LlmFeedbackProperties {

    /**
     * LLM feedback parsing scaffold flag. The default stays disabled so the
     * current rule-based parser remains the only active behavior.
     */
    private final Feedback feedback = new Feedback();
    // 좌표(x/z/rotation)까지 LLM이 직접 생성하는 배치 추천 플래그. 기본값은
    // false — 켜지기 전까지는 RuleBasedPlacementService만 동작한다.
    private final Placement placement = new Placement();
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

    public Placement getPlacement() {
        return placement;
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
        return hasUsableApiKey(apiKey) && hasHttpBaseUrl(baseUrl) && hasText(model);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasUsableApiKey(String value) {
        if (!hasText(value)) return false;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !normalized.equals("placeholder") && !normalized.equals("your_api_key")
                && !normalized.equals("change-me") && !normalized.equals("changeme");
    }

    private boolean hasHttpBaseUrl(String value) {
        if (!hasText(value)) return false;
        try {
            URI uri = new URI(value.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
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

    public static class Placement {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
