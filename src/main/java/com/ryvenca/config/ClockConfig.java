package com.ryvenca.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Season and "today's suggestions" follow the Turkish calendar day. */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Istanbul"));
    }
}
