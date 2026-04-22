package com.mdd.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the application clock so time-dependent code remains testable.
 */
@Configuration
public class TimeConfig {

    /**
     * Provides the single production clock used by time-dependent services.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
