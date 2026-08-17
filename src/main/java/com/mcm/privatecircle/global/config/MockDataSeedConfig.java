package com.mcm.privatecircle.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MockDataProperties.class)
public class MockDataSeedConfig {
}
