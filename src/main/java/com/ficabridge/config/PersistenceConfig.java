package com.ficabridge.config;

import org.springframework.context.annotation.Configuration;

/**
 * JPA / datasource configuration.
 * In the local profile, datasource is provided by Docker Compose PostgreSQL.
 * In the btp profile, datasource credentials are injected from VCAP_SERVICES.
 */
@Configuration
public class PersistenceConfig {
}
