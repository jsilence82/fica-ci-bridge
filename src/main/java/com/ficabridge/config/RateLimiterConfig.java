package com.ficabridge.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the "sapOData" RateLimiter instance (configured in application.yml under
 * resilience4j.ratelimiter.instances.sapOData) so it can be injected into the OData
 * client layer without any client needing to know about the RateLimiterRegistry.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiter sapODataRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        return rateLimiterRegistry.rateLimiter("sapOData");
    }
}
