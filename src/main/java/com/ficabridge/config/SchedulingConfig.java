package com.ficabridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} methods, currently only {@code sync.DocumentSyncScheduler}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
