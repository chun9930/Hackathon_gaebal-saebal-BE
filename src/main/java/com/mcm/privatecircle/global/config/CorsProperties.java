package com.mcm.privatecircle.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
    List<String> allowedOriginPatterns,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    List<String> exposedHeaders,
    Boolean allowCredentials,
    Long maxAgeSeconds
) {

    public CorsProperties {
        allowedOriginPatterns = copyOrDefault(allowedOriginPatterns, List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        allowedMethods = copyOrDefault(allowedMethods, List.of(
            "GET",
            "POST",
            "PATCH",
            "PUT",
            "DELETE",
            "OPTIONS"
        ));
        allowedHeaders = copyOrDefault(allowedHeaders, List.of("*"));
        exposedHeaders = copyOrDefault(exposedHeaders, List.of("Authorization"));
        allowCredentials = allowCredentials == null ? Boolean.TRUE : allowCredentials;
        maxAgeSeconds = maxAgeSeconds == null ? 3600L : maxAgeSeconds;
    }

    private static List<String> copyOrDefault(List<String> source, List<String> fallback) {
        if (source == null || source.isEmpty()) {
            return fallback;
        }
        return List.copyOf(source);
    }
}
