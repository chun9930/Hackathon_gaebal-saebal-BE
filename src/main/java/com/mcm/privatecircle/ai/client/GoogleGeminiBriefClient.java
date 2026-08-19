package com.mcm.privatecircle.ai.client;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Schema;
import com.mcm.privatecircle.ai.config.GeminiProperties;
import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.ai.dto.GeminiBriefResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoogleGeminiBriefClient implements GeminiBriefClient {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final RawJsonRequester rawJsonRequester;

    @Autowired
    public GoogleGeminiBriefClient(GeminiProperties properties) {
        this(properties, createSdkRequester(properties));
    }

    public GoogleGeminiBriefClient(GeminiProperties properties, RawJsonRequester rawJsonRequester) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.rawJsonRequester = rawJsonRequester;
    }

    @Override
    public GeminiBriefResult generate(AiBriefSource source) {
        String rawJson = requestRawJson(buildPrompt(source));
        return parseAndValidate(rawJson);
    }

    protected String requestRawJson(String prompt) {
        try {
            String rawJson = rawJsonRequester.request(prompt);
            if (rawJson == null || rawJson.isBlank()) {
                throw new AiClientException("Gemini returned blank response");
            }
            return rawJson;
        } catch (AiClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isTimeout(exception)) {
                throw new AiClientTimeoutException("Gemini request timed out", exception);
            }
            throw new AiClientException("Gemini external call failed", exception);
        } catch (Exception exception) {
            if (isTimeout(exception)) {
                throw new AiClientTimeoutException("Gemini request timed out", exception);
            }
            throw new AiClientException("Gemini external call failed", exception);
        }
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

    private static RawJsonRequester createSdkRequester(GeminiProperties properties) {
        return prompt -> {
            HttpOptions httpOptions = HttpOptions.builder()
                .timeout(Math.toIntExact(properties.timeout().toMillis()))
                .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(buildResponseSchema())
                .httpOptions(httpOptions)
                .build();

            try (Client client = Client.builder()
                .apiKey(properties.apiKey())
                .httpOptions(httpOptions)
                .build()) {
                return client.models.generateContent(properties.model(), prompt, config).text();
            }
        };
    }

    private static Schema buildResponseSchema() {
        Schema stringSchema = Schema.builder()
            .type("STRING")
            .build();

        return Schema.builder()
            .type("OBJECT")
            .properties(Map.of(
                "summary", stringSchema,
                "visitPurposeSummary", stringSchema,
                "interestSummary", stringSchema,
                "cautionSummary", stringSchema,
                "suggestedDirection", stringSchema
            ))
            .required(List.of(
                "summary",
                "visitPurposeSummary",
                "interestSummary",
                "cautionSummary",
                "suggestedDirection"
            ))
            .propertyOrdering(
                "summary",
                "visitPurposeSummary",
                "interestSummary",
                "cautionSummary",
                "suggestedDirection"
            )
            .build();
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AiClientTimeoutException
                || current instanceof HttpTimeoutException
                || current instanceof SocketTimeoutException
                || current instanceof InterruptedIOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @FunctionalInterface
    public interface RawJsonRequester {
        String request(String prompt) throws Exception;
    }
}




