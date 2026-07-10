package com.roomfit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.llm.OpenAiCompatibleLlmClient;
import com.roomfit.placement.FallbackFeedbackIntentParser;
import com.roomfit.placement.FeedbackIntentParser;
import com.roomfit.placement.LlmFeedbackIntentParser;
import com.roomfit.placement.RuleBasedFeedbackIntentParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties(LlmFeedbackProperties.class)
public class FeedbackIntentParserConfig {

    @Bean
    public FeedbackIntentParser feedbackIntentParser(LlmFeedbackProperties properties,
                                                     ObjectMapper objectMapper) {
        RuleBasedFeedbackIntentParser ruleBasedParser = new RuleBasedFeedbackIntentParser();
        Optional<FeedbackIntentParser> llmParser = createLlmParser(properties, objectMapper);
        return new FallbackFeedbackIntentParser(llmParser, ruleBasedParser, properties);
    }

    private Optional<FeedbackIntentParser> createLlmParser(LlmFeedbackProperties properties,
                                                           ObjectMapper objectMapper) {
        if (!properties.isEnabled() || !properties.hasValidClientConfig()) {
            return Optional.empty();
        }
        return Optional.of(new LlmFeedbackIntentParser(
                new OpenAiCompatibleLlmClient(properties, objectMapper)));
    }
}
