package com.mcm.privatecircle.ai.client;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcm.privatecircle.ai.config.GeminiProperties;
import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.ai.dto.GeminiBriefResult;

import org.springframework.stereotype.Component;

@Component
public class GoogleGeminiBriefClient implements GeminiBriefClient {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GoogleGeminiBriefClient(GeminiProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public GeminiBriefResult generate(AiBriefSource source) {
        String rawJson = requestRawJson(buildPrompt(source));
        return parseAndValidate(rawJson);
    }

    protected String requestRawJson(String prompt) {
        throw new AiClientException(
            "Google Gemini external call is not enabled in the current safe local mode."
        );
    }

    private GeminiBriefResult parseAndValidate(String rawJson) {
        try {
            Map<String, String> values = objectMapper.readValue(rawJson, STRING_MAP);
            String summary = requireText(values, "summary");
            String visitPurposeSummary = requireText(values, "visitPurposeSummary");
            String interestSummary = requireText(values, "interestSummary");
            String cautionSummary = requireText(values, "cautionSummary");
            String suggestedDirection = requireText(values, "suggestedDirection");
            if (values.size() != 5) {
                throw new AiResponseParseException("Unexpected Gemini response fields");
            }
            return new GeminiBriefResult(
                summary,
                visitPurposeSummary,
                interestSummary,
                cautionSummary,
                suggestedDirection
            );
        } catch (JsonProcessingException exception) {
            throw new AiResponseParseException("Failed to parse Gemini response", exception);
        }
    }

    private String requireText(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new AiResponseParseException("Missing or blank field: " + key);
        }
        return value;
    }

    private String buildPrompt(AiBriefSource source) {
        try {
            String payload = objectMapper.writeValueAsString(source);
            return "You are generating a concise CA journey brief. "
                + "Use only the provided JSON data. "
                + "Do not infer facts that are not present. "
                + "Return valid JSON with exactly these keys: "
                + "summary, visitPurposeSummary, interestSummary, cautionSummary, suggestedDirection. "
                + "All values must be non-empty Korean strings.\n"
                + "Model=" + properties.model() + "\n"
                + payload;
        } catch (JsonProcessingException exception) {
            throw new AiClientException("Failed to serialize AI source", exception);
        }
    }
}
