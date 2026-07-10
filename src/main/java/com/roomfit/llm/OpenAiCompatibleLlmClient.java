package com.roomfit.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.config.LlmFeedbackProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OpenAiCompatibleLlmClient implements LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String model;

    public OpenAiCompatibleLlmClient(LlmFeedbackProperties properties, ObjectMapper objectMapper) {
        this(properties, restClientBuilder(properties), objectMapper);
    }

    OpenAiCompatibleLlmClient(LlmFeedbackProperties properties,
                              RestClient.Builder restClientBuilder,
                              ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = objectMapper;
        this.endpoint = normalizeEndpoint(properties.getBaseUrl());
        this.model = properties.getModel();
    }

    @Override
    public String complete(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt
                ))
        );

        String responseBody = restClient.post()
                .uri(endpoint)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return stripMarkdownCodeFence(extractText(responseBody));
    }

    private String extractText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("intent")) {
                return responseBody;
            }

            JsonNode firstChoice = root.path("choices").path(0);
            String messageContent = firstChoice.path("message").path("content").asText("");
            if (!messageContent.isBlank()) {
                return messageContent;
            }
            return firstChoice.path("text").asText("");
        } catch (JsonProcessingException e) {
            return responseBody;
        }
    }

    private String stripMarkdownCodeFence(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        String withoutOpeningFence = trimmed.replaceFirst("^```(?:json)?\\s*", "");
        return withoutOpeningFence.replaceFirst("\\s*```$", "").trim();
    }

    private static RestClient.Builder restClientBuilder(LlmFeedbackProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(1, properties.getTimeoutMs());
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return RestClient.builder().requestFactory(requestFactory);
    }

    private String normalizeEndpoint(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/chat/completions";
        }
        return trimmed + "/v1/chat/completions";
    }
}
