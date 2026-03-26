package com.ficabridge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI / Swagger UI configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FI-CA CI Bridge API")
                        .version("1.0.0")
                        .description("Anti-Corruption Layer between SAP FI-CA / Convergent Invoicing and REST consumers"));
    }
}
