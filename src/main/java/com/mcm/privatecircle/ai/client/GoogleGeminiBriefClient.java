package com.mcm.privatecircle.ai.client;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.mcm.privatecircle.ai.config.GeminiProperties;
import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.ai.dto.GeminiBriefResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoogleGeminiBriefClient implements GeminiBriefClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleGeminiBriefClient.class);

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final Client client;

    public GoogleGeminiBriefClient(GeminiProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        String environmentApiKey = System.getenv("GOOGLE_API_KEY");
        String configuredApiKey = properties.apiKey();
        log.info("[AI BRIEF] GOOGLE_API_KEY loaded: {}, length: {}",
            hasText(environmentApiKey), lengthOf(environmentApiKey));
        log.info("[AI BRIEF] Configured Gemini API key loaded: {}, length: {}",
            hasText(configuredApiKey), lengthOf(configuredApiKey));
        log.info("[AI BRIEF] Conflicting Google auth environment present: enterprise={}, vertex={}, credentials={}, project={}, location={}, customBaseUrl={}",
            isEnabled("GOOGLE_GENAI_USE_ENTERPRISE"), isEnabled("GOOGLE_GENAI_USE_VERTEXAI"),
            hasEnvironmentValue("GOOGLE_APPLICATION_CREDENTIALS"), hasEnvironmentValue("GOOGLE_CLOUD_PROJECT"),
            hasEnvironmentValue("GOOGLE_CLOUD_LOCATION"), hasEnvironmentValue("GOOGLE_GEMINI_BASE_URL"));
        this.client = hasText(configuredApiKey)
            ? Client.builder()
                .apiKey(configuredApiKey.trim())
                .enterprise(false)
                .vertexAI(false)
                .httpOptions(HttpOptions.builder().timeout(Math.toIntExact(properties.timeout().toMillis())).build())
                .build()
            : null;
    }

    @Override
    public GeminiBriefResult generate(AiBriefSource source) {
        String rawJson = requestRawJson(buildPrompt(source));
        return parseAndValidate(rawJson);
    }

    protected String requestRawJson(String prompt) {
        if (client == null) {
            throw new AiClientConfigurationException("GOOGLE_API_KEY is missing");
        }
        GenerateContentConfig config = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(buildResponseSchema())
            .build();

        String responseText;
        try {
            log.info("[AI BRIEF] Gemini request started: model={}, timeoutMs={}, promptLength={}",
                properties.model(), properties.timeout().toMillis(), prompt.length());
            responseText = client.models.generateContent(properties.model(), prompt, config).text();
        } catch (ApiException exception) {
            log.warn("[AI BRIEF] Gemini API rejected request: statusCode={}, status={}",
                exception.code(), exception.status());
            if (exception.code() == 401) {
                throw new AiClientAuthenticationException("Gemini authentication failed", exception);
            }
            throw new AiClientException("Gemini API request failed", exception);
        } catch (GenAiIOException exception) {
            log.warn("[AI BRIEF] Gemini request failed: model={}, message={}",
                properties.model(), exception.getMessage());
            throw new AiClientTimeoutException("Gemini request timed out", exception);
        }
        if (responseText == null || responseText.isBlank()) {
            throw new AiClientException("Gemini returned an empty response");
        }
        log.info("[AI BRIEF] Gemini response received: responseLength={}", responseText.length());
        return responseText;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    private boolean hasEnvironmentValue(String name) {
        return hasText(System.getenv(name));
    }

    private boolean isEnabled(String name) {
        return Boolean.parseBoolean(System.getenv(name));
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
            String currentVisitRecord = source.currentVisitRecord() == null
                ? "currentVisitRecord: none"
                : "currentVisitRecord: visitedAt=" + source.currentVisitRecord().visitedAt()
                    + ", visitPurpose=" + safeText(source.currentVisitRecord().visitPurpose())
                    + ", content=" + safeText(source.currentVisitRecord().content())
                    + ", styleChangeNote=" + safeText(source.currentVisitRecord().styleChangeNote())
                    + ", cautionNote=" + safeText(source.currentVisitRecord().cautionNote());
            return "You are generating a concise CA journey brief. "
                + "Use only the provided JSON data. "
                + "Do not infer facts that are not present. "
                + "The latest consultation record is the target visit record. "
                + "If currentVisitRecord.cautionNote is present, cautionSummary must explicitly reflect that latest caution. "
                + "Do not replace it with a generic omission or 'no caution' message. "
                + "Use currentVisitRecord together with past visitRecords. "
                + "Return valid JSON with exactly these keys: "
                + "summary, visitPurposeSummary, interestSummary, cautionSummary, suggestedDirection. "
                + "All values must be non-empty Korean strings.\n"
                + currentVisitRecord + "\n"
                + "Model=" + properties.model() + "\n"
                + payload;
        } catch (JsonProcessingException exception) {
            throw new AiClientException("Failed to serialize AI source", exception);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private Schema buildResponseSchema() {
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("summary", textSchema("summary"));
        properties.put("visitPurposeSummary", textSchema("visitPurposeSummary"));
        properties.put("interestSummary", textSchema("interestSummary"));
        properties.put("cautionSummary", textSchema("cautionSummary"));
        properties.put("suggestedDirection", textSchema("suggestedDirection"));

        return Schema.builder()
            .type(Type.Known.OBJECT)
            .properties(properties)
            .required(
                "summary",
                "visitPurposeSummary",
                "interestSummary",
                "cautionSummary",
                "suggestedDirection"
            )
            .build();
    }

    private Schema textSchema(String description) {
        return Schema.builder()
            .type(Type.Known.STRING)
            .description(description)
            .build();
    }
}
