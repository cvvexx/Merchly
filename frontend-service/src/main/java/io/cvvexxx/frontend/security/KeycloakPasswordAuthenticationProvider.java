package io.cvvexxx.frontend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cvvexxx.frontend.client.keycloak.KeycloakRestClient;
import io.cvvexxx.frontend.dto.KeycloakTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakPasswordAuthenticationProvider implements AuthenticationProvider {

    private final KeycloakRestClient KeycloakrestClient;
    private final ObjectMapper objectMapper; // Автоматически внедряется Spring Boot'ом

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        try {
            KeycloakTokenResponse tokenResponse = KeycloakrestClient.login(username, password);

            List<GrantedAuthority> authorities = getAuthorities(tokenResponse);

            return new KeycloakJwtAuthenticationToken(
                    username,
                    tokenResponse.accessToken(),
                    authorities
            );
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", username, e);
            throw new BadCredentialsException("Неверный логин или пароль", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private List<GrantedAuthority> getAuthorities(KeycloakTokenResponse tokenResponse) {
        String accessToken = tokenResponse.accessToken();
        log.debug("accessToken: {}", accessToken);
        if (accessToken == null || accessToken.isBlank()) {
            return Collections.emptyList();
        }

        try {
            String[] chunks = accessToken.split("\\.");
            if (chunks.length < 2) {
                return Collections.emptyList();
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(chunks[1]));

            JsonNode rootNode = objectMapper.readTree(payloadJson);
            JsonNode realmAccessNode = rootNode.path("realm_access").path("roles");

            List<GrantedAuthority> authorities = new ArrayList<>();

            if (realmAccessNode.isArray()) {
                for (JsonNode roleNode : realmAccessNode) {
                    String roleName = roleNode.asText();
                    String authorityName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    authorities.add(new SimpleGrantedAuthority(authorityName));
                }
            }
            log.info("Authorities: {}", authorities);
            return authorities;
        } catch (Exception e) {
            log.error("Failed to parse roles from JWT token", e);
            return Collections.emptyList();
        }
    }
}