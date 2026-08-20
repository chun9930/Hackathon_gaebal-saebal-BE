package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.Duration;

import com.mcm.privatecircle.ai.client.AiResponseParseException;
import com.mcm.privatecircle.ai.client.GoogleGeminiBriefClient;
import com.mcm.privatecircle.ai.config.GeminiProperties;
import com.mcm.privatecircle.ai.dto.AiBriefSource;

import org.junit.jupiter.api.Test;

class GoogleGeminiBriefClientTest {

    private final AiBriefSource source = new AiBriefSource(
        new AiBriefSource.CustomerProfile("VIP", "black"),
        null,
        java.util.List.of(),
        java.util.List.of(),
        java.util.List.of(),
        0
    );

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

    @Test
    void generateIncludesCurrentVisitCautionInPrompt() {
        AiBriefSource sourceWithCaution = new AiBriefSource(
            new AiBriefSource.CustomerProfile("VIP", "black"),
            new AiBriefSource.VisitRecordSource(
                LocalDateTime.of(2026, 8, 19, 14, 0),
                "상담 방문",
                "오늘은 가방과 지갑을 함께 비교했습니다.",
                "차분한 응대 선호",
                "강한 권유는 피해야 함"
            ),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            0
        );
        CapturingGoogleGeminiBriefClient client = new CapturingGoogleGeminiBriefClient(
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

        client.generate(sourceWithCaution);

        assertThat(client.getLastPrompt())
            .contains("currentVisitRecord.cautionNote")
            .contains("강한 권유는 피해야 함");
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

    private static final class CapturingGoogleGeminiBriefClient extends GoogleGeminiBriefClient {

        private final String rawJson;
        private String lastPrompt;

        private CapturingGoogleGeminiBriefClient(String rawJson) {
            super(new GeminiProperties("test-key", "gemini-3.6-flash", Duration.ofSeconds(30)));
            this.rawJson = rawJson;
        }

        @Override
        protected String requestRawJson(String prompt) {
            this.lastPrompt = prompt;
            return rawJson;
        }

        private String getLastPrompt() {
            return lastPrompt;
        }
    }
}
