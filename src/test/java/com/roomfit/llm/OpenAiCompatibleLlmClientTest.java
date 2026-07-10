package com.roomfit.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.config.LlmFeedbackProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleLlmClientTest {

    @Test
    void complete_extractsMessageContentAndStripsMarkdownFence() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                properties("https://llm.example.com", "test-key", "test-model"),
                restClientBuilder,
                new ObjectMapper()
        );

        server.expect(requestTo("https://llm.example.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().string(containsString("\"model\":\"test-model\"")))
                .andExpect(content().string(containsString("JSON only")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "```json\\n{\\n  \\"intent\\": \\"ENLARGE_FURNITURE\\"\\n}\\n```"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String result = client.complete("Return JSON only");

        assertThat(result).isEqualTo("""
                {
                  "intent": "ENLARGE_FURNITURE"
                }
                """.trim());
        server.verify();
    }

    @Test
    void complete_acceptsBaseUrlEndingWithV1() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                properties("https://llm.example.com/v1", "test-key", "test-model"),
                restClientBuilder,
                new ObjectMapper()
        );

        server.expect(requestTo("https://llm.example.com/v1/chat/completions"))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"intent\\":\\"INCREASE_STORAGE\\"}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.complete("prompt")).isEqualTo("{\"intent\":\"INCREASE_STORAGE\"}");
        server.verify();
    }

    private LlmFeedbackProperties properties(String baseUrl, String apiKey, String model) {
        LlmFeedbackProperties properties = new LlmFeedbackProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiKey(apiKey);
        properties.setModel(model);
        return properties;
    }
}
