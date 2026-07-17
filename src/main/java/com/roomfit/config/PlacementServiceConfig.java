package com.roomfit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.llm.OpenAiCompatibleLlmClient;
import com.roomfit.placement.FallbackPlacementService;
import com.roomfit.placement.LlmPlacementService;
import com.roomfit.placement.PlacementService;
import com.roomfit.placement.RuleBasedPlacementService;
import com.roomfit.placement.ValidationService;
import com.roomfit.product.service.MockProductService;
import com.roomfit.product.service.ProductRecommendationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * FeedbackIntentParserConfig와 동일한 빈-선택 패턴: roomfit.llm.placement.enabled와
 * LLM 클라이언트 설정(apiKey/baseUrl/model)이 모두 유효할 때만 LlmPlacementService를
 * 활성화하고, 그 외에는 항상 RuleBasedPlacementService만 동작한다.
 */
@Configuration
public class PlacementServiceConfig {

    @Bean
    public PlacementService placementService(LlmFeedbackProperties properties,
                                              ObjectMapper objectMapper,
                                              MockProductService mockProductService,
                                              ProductRecommendationService productRecommendationService,
                                              ValidationService validationService) {
        RuleBasedPlacementService ruleBasedService =
                new RuleBasedPlacementService(mockProductService, productRecommendationService);
        Optional<PlacementService> llmService = createLlmService(properties, objectMapper, mockProductService, validationService);
        return new FallbackPlacementService(llmService, ruleBasedService, properties);
    }

    private Optional<PlacementService> createLlmService(LlmFeedbackProperties properties,
                                                          ObjectMapper objectMapper,
                                                          MockProductService mockProductService,
                                                          ValidationService validationService) {
        if (!properties.getPlacement().isEnabled() || !properties.hasValidClientConfig()) {
            return Optional.empty();
        }
        return Optional.of(new LlmPlacementService(
                new OpenAiCompatibleLlmClient(properties, objectMapper),
                validationService,
                mockProductService,
                objectMapper));
    }
}
