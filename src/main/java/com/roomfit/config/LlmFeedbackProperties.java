package com.roomfit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "roomfit.llm.feedback")
public class LlmFeedbackProperties {

    /**
     * LLM feedback parsing scaffold flag. The default stays disabled so the
     * current rule-based parser remains the only active behavior.
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
