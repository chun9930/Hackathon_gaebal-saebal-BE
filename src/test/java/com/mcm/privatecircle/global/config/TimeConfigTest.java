package com.mcm.privatecircle.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TimeConfigTest {

    @Test
    void 애플리케이션_Clock은_Asia_Seoul을_사용한다() {
        Clock clock = new TimeConfig().applicationClock();

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
