package com.mcm.privatecircle.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mock-data")
public record MockDataProperties(
    boolean seedEnabled,
    String storesResource,
    String productsResource
) {
}
