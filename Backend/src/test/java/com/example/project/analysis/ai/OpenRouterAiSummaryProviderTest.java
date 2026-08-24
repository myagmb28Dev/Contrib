package com.example.project.analysis.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.project.analysis.calculator.AnalysisMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenRouterAiSummaryProviderTest {

    private OpenRouterProperties properties;
    private MockRestServiceServer server;
    private OpenRouterAiSummaryProvider provider;

    @BeforeEach
    void setUp() {
        properties = new OpenRouterProperties();
        properties.setApiKey("test-key");
        properties.setSiteUrl("https://contrib.example");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OpenRouterAiSummaryProvider(builder, new ObjectMapper(), properties);
    }

    @Test
    void requestsNemotronWithStrictJsonSchemaAndParsesResult() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(header("HTTP-Referer", "https://contrib.example"))
                .andExpect(header("X-OpenRouter-Title", "Contrib"))
                .andExpect(jsonPath("$.model").value("nvidia/nemotron-3-super-120b-a12b:free"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andRespond(withSuccess("""
                        {
                          "model": "nvidia/nemotron-3-super-120b-a12b:free",
                          "choices": [{
                            "message": {
                              "content": "{\\\"summary\\\":\\\"Java 저장소에서 꾸준히 구현과 협업에 참여했어.\\\",\\\"technicalAreas\\\":[\\\"Java\\\",\\\"협업\\\",\\\"Java\\\"]}"
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        AnalysisMetrics metrics = new AnalysisMetrics(12, 3, 2, 4, 8, 30, 900, 120);
        AiSummaryResult result = provider.summarize(new AiSummaryInput("owner/repo", "Java", metrics, 74));

        assertThat(result.summary()).contains("구현과 협업");
        assertThat(result.technicalAreas()).containsExactly("Java", "협업");
        assertThat(result.model()).isEqualTo("nvidia/nemotron-3-super-120b-a12b:free");
        assertThat(result.promptVersion()).isEqualTo("openrouter-nemotron-v1");
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeSendingRequest() {
        properties.setApiKey(" ");

        assertThatThrownBy(() -> provider.summarize(input()))
                .isInstanceOf(OpenRouterAiException.class)
                .hasMessageContaining("OPENROUTER_API_KEY");
    }

    @Test
    void rejectsMalformedStructuredOutput() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"{\\\"summary\\\":\\\"ok\\\",\\\"technicalAreas\\\":[]}"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.summarize(input()))
                .isInstanceOf(OpenRouterAiException.class)
                .hasMessageContaining("technical areas");
        server.verify();
    }

    private AiSummaryInput input() {
        return new AiSummaryInput("owner/repo", "Java", new AnalysisMetrics(1, 0, 0, 0, 1, 1, 10, 2), 2);
    }
}
