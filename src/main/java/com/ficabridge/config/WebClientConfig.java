package com.ficabridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures a Spring WebClient pre-loaded with the SAP OData base URL
 * and Basic auth credentials. In BTP, credentials come from the XSUAA binding.
 */
@Configuration
public class WebClientConfig {

    @Value("${sap.odata.base-url:http://localhost:8089}")
    private String baseUrl;

    @Value("${sap.odata.username:wiremock}")
    private String username;

    @Value("${sap.odata.password:wiremock}")
    private String password;

    @Bean
    public WebClient sapODataWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }
}
