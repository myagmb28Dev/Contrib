package com.example.project.analysis.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenRouterAiSummaryProvider implements AiSummaryProvider {

    static final String PROMPT_VERSION = "openrouter-nemotron-v2";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenRouterProperties properties;

    public OpenRouterAiSummaryProvider(RestClient.Builder builder, ObjectMapper objectMapper,
            OpenRouterProperties properties) {
        this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String providerName() {
        return "openrouter";
    }

    @Override
    public AiSummaryResult summarize(AiSummaryInput input) {
        requireConfiguration();
        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .headers(headers -> addOptionalHeaders(headers))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody(input))
                    .retrieve()
                    .body(JsonNode.class);
            return parseResponse(response);
        } catch (OpenRouterAiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OpenRouterAiException("OpenRouter summary request failed", exception);
        }
    }

    private void requireConfiguration() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new OpenRouterAiException("OPENROUTER_API_KEY is not configured");
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new OpenRouterAiException("OPENROUTER_MODEL is not configured");
        }
    }

    private void addOptionalHeaders(HttpHeaders headers) {
        if (properties.getSiteUrl() != null && !properties.getSiteUrl().isBlank()) {
            headers.set("HTTP-Referer", properties.getSiteUrl());
        }
        if (properties.getAppName() != null && !properties.getAppName().isBlank()) {
            headers.set("X-OpenRouter-Title", properties.getAppName());
        }
    }

    private ObjectNode requestBody(AiSummaryInput input) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("temperature", 0.1);
        body.put("max_tokens", 500);

        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "GitHub 기여 지표를 사실에 근거하여 요약합니다. "
                        + "과장하거나 입력에 없는 활동을 추측하지 말고, summary는 정중한 서술형 한국어 문체로 2~3문장을 작성합니다. "
                        + "technicalAreas는 입력 언어와 지표로 확인 가능한 영역만 짧은 문자열로 최대 6개 반환합니다.");
        messages.addObject()
                .put("role", "user")
                .put("content", objectMapper.valueToTree(input).toString());

        ObjectNode schema = body.putObject("response_format")
                .put("type", "json_schema")
                .putObject("json_schema");
        schema.put("name", "contribution_summary");
        schema.put("strict", true);
        ObjectNode definition = schema.putObject("schema");
        definition.put("type", "object");
        definition.put("additionalProperties", false);
        ObjectNode fields = definition.putObject("properties");
        fields.putObject("summary")
                .put("type", "string")
                .put("minLength", 1)
                .put("maxLength", 800);
        ObjectNode areas = fields.putObject("technicalAreas");
        areas.put("type", "array");
        areas.put("minItems", 1);
        areas.put("maxItems", 6);
        areas.putObject("items").put("type", "string").put("minLength", 1).put("maxLength", 60);
        definition.putArray("required").add("summary").add("technicalAreas");
        return body;
    }

    private AiSummaryResult parseResponse(JsonNode response) {
        if (response == null) {
            throw new OpenRouterAiException("OpenRouter returned an empty response");
        }
        String content = response.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new OpenRouterAiException("OpenRouter response did not contain a message");
        }

        try {
            JsonNode result = objectMapper.readTree(content);
            String summary = result.path("summary").asText("").trim();
            JsonNode areaNodes = result.path("technicalAreas");
            if (summary.isBlank() || summary.length() > 800 || !areaNodes.isArray()) {
                throw new OpenRouterAiException("OpenRouter returned an invalid summary payload");
            }

            Set<String> uniqueAreas = new LinkedHashSet<>();
            for (JsonNode areaNode : areaNodes) {
                String area = areaNode.asText("").trim();
                if (!area.isBlank() && area.length() <= 60) {
                    uniqueAreas.add(area);
                }
                if (uniqueAreas.size() == 6) {
                    break;
                }
            }
            if (uniqueAreas.isEmpty()) {
                throw new OpenRouterAiException("OpenRouter returned no technical areas");
            }

            String model = response.path("model").asText(properties.getModel());
            return new AiSummaryResult(summary, List.copyOf(new ArrayList<>(uniqueAreas)), model, PROMPT_VERSION);
        } catch (OpenRouterAiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OpenRouterAiException("Could not parse OpenRouter response", exception);
        }
    }
}
