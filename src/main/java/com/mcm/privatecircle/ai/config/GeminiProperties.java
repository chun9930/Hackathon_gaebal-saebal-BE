package com.mcm.privatecircle.ai.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.ai.gemini")
public record GeminiProperties(
    String apiKey,
    @NotBlank String model,
    @NotNull Duration timeout
) {
}
