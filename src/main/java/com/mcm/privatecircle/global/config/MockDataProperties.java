package com.mcm.privatecircle.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mock-data")
public record MockDataProperties(
    boolean seedEnabled,
    String storesResource,
    String productsResource,
    boolean employeeSeedEnabled,
    List<EmployeeSeedProperties> employees
) {

    public record EmployeeSeedProperties(
        String loginId,
        String password,
        String name,
        String storeName
    ) {
    }
}
