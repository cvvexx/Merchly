package io.cvvexxx.users.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    public static final String ADMIN_REALM = "master";

    @Value("${keycloak.server-url}")
    private String serverUrl;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.admin-username}")
    private String username;
    @Value("${keycloak.admin-password}")
    private String password;

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(ADMIN_REALM)
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();
    }

}
