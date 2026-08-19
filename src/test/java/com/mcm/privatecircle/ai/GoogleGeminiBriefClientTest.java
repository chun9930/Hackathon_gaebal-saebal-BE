package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import com.mcm.privatecircle.ai.client.AiClientTimeoutException;
import com.mcm.privatecircle.ai.client.AiResponseParseException;
import com.mcm.privatecircle.ai.client.GoogleGeminiBriefClient;
import com.mcm.privatecircle.ai.config.GeminiProperties;
import com.mcm.privatecircle.ai.dto.AiBriefSource;

import org.junit.jupiter.api.Test;

class GoogleGeminiBriefClientTest {

    private final AiBriefSource source = new AiBriefSource(
        new AiBriefSource.CustomerProfile("VIP", "black"),
        java.util.List.of(),
        java.util.List.of(),
        java.util.List.of(),
        0
    );

    @Test
    void generateUsesConfiguredRequesterToObtainRawJson() {
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        GoogleGeminiBriefClient client = new GoogleGeminiBriefClient(
            new GeminiProperties("test-key", "gemini-3.6-flash", Duration.ofSeconds(30)),
            prompt -> {
                capturedPrompt.set(prompt);
                return """
                    {
                      \"summary\": \"summary\",
                      \"visitPurposeSummary\": \"purpose\",
                      \"interestSummary\": \"interest\",
                      \"cautionSummary\": \"caution\",
                      \"suggestedDirection\": \"direction\"
                    }
                    """;
            }
        );

        var result = client.generate(source);

        assertThat(capturedPrompt.get()).contains("Model=gemini-3.6-flash");
        assertThat(capturedPrompt.get()).contains("VIP");
        assertThat(result.summary()).isEqualTo("summary");
    }

    @Test
    void generateMapsTimeoutFailuresToAiClientTimeoutException() {
        GoogleGeminiBriefClient client = new GoogleGeminiBriefClient(
            new GeminiProperties("test-key", "gemini-3.6-flash", Duration.ofSeconds(30)),
            prompt -> {
                throw new RuntimeException(new HttpTimeoutException("timeout"));
            }
        );

        assertThatThrownBy(() -> client.generate(source))
            .isInstanceOf(AiClientTimeoutException.class);
    }

    @Test
    void generateParsesValidJson() {
        GoogleGeminiBriefClient client = new FakeGoogleGeminiBriefClient(
            """
            {
              "summary": "summary",
              "visitPurposeSummary": "purpose",
              "interestSummary": "interest",
              "cautionSummary": "caution",
              "suggestedDirection": "direction"
            }
            """
        );

        var result = client.generate(source);

        assertThat(result.summary()).isEqualTo("summary");
        assertThat(result.visitPurposeSummary()).isEqualTo("purpose");
        assertThat(result.interestSummary()).isEqualTo("interest");
        assertThat(result.cautionSummary()).isEqualTo("caution");
        assertThat(result.suggestedDirection()).isEqualTo("direction");
    }

    @Test
    void generateRejectsMissingField() {
        GoogleGeminiBriefClient client = new FakeGoogleGeminiBriefClient(
            """
            {
              "summary": "summary",
              "visitPurposeSummary": "purpose",
              "interestSummary": "interest",
              "cautionSummary": "caution"
            }
            """
        );

        assertThatThrownBy(() -> client.generate(source))
            .isInstanceOf(AiResponseParseException.class);
    }

    @Test
    void generateRejectsBlankField() {
        GoogleGeminiBriefClient client = new FakeGoogleGeminiBriefClient(
            """
            {
              "summary": "summary",
              "visitPurposeSummary": "purpose",
              "interestSummary": "interest",
              "cautionSummary": "caution",
              "suggestedDirection": " "
            }
            """
        );

        assertThatThrownBy(() -> client.generate(source))
            .isInstanceOf(AiResponseParseException.class);
    }

    private static final class FakeGoogleGeminiBriefClient extends GoogleGeminiBriefClient {

        private final String rawJson;

        private FakeGoogleGeminiBriefClient(String rawJson) {
            super(new GeminiProperties("test-key", "gemini-3.6-flash", Duration.ofSeconds(30)));
            this.rawJson = rawJson;
        }

        @Override
        protected String requestRawJson(String prompt) {
            return rawJson;
        }
    }
}
