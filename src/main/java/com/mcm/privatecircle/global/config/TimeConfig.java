package com.mcm.privatecircle.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock applicationClock() {
        return Clock.system(APPLICATION_ZONE);
    }
}
