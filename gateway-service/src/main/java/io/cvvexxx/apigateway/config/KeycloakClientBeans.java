package io.cvvexxx.apigateway.config;

import io.cvvexxx.apigateway.client.KeycloakRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KeycloakClientBeans {

    @Bean
    public KeycloakRestClient keycloakRestClient(
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.token-uri}") String tokenUri,
            @Value("${keycloak.client-secret}") String clientSecret,
            @Value("${keycloak.logout-uri}") String logoutUri
    ) {
        return new KeycloakRestClient(
                RestClient.builder().build(),
                clientId,
                tokenUri,
                clientSecret,
                logoutUri
        );
    }
}
