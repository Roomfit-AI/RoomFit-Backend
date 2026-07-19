package com.roomfit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.llm.OpenAiCompatibleLlmClient;
import com.roomfit.placement.FallbackFeedbackPlanInterpreter;
import com.roomfit.placement.FeedbackPlanInterpreter;
import com.roomfit.placement.LlmFeedbackPlanInterpreter;
import com.roomfit.placement.RuleBasedFeedbackPlanInterpreter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class FeedbackPlanConfig {

    @Bean
    public FeedbackPlanInterpreter feedbackPlanInterpreter(LlmFeedbackProperties properties, ObjectMapper objectMapper) {
        LlmFeedbackProperties feedbackProperties = properties.feedbackClientProperties();
        Optional<FeedbackPlanInterpreter> llm = properties.isEnabled() && properties.hasValidFeedbackClientConfig()
                ? Optional.of(new LlmFeedbackPlanInterpreter(
                        OpenAiCompatibleLlmClient.forFeedback(feedbackProperties, objectMapper), objectMapper))
                : Optional.empty();
        return new FallbackFeedbackPlanInterpreter(llm, new RuleBasedFeedbackPlanInterpreter());
    }
}
