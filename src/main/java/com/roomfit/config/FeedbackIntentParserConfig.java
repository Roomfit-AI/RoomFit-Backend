package com.roomfit.config;

import com.roomfit.placement.FallbackFeedbackIntentParser;
import com.roomfit.placement.FeedbackIntentParser;
import com.roomfit.placement.RuleBasedFeedbackIntentParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmFeedbackProperties.class)
public class FeedbackIntentParserConfig {

    @Bean
    public FeedbackIntentParser feedbackIntentParser(LlmFeedbackProperties properties) {
        return new FallbackFeedbackIntentParser(new RuleBasedFeedbackIntentParser(), properties);
    }
}
