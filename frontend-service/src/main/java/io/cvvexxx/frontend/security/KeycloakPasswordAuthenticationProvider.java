package io.cvvexxx.frontend.security;

import com.fasterxml.jackson.databind.JsonNode;
import io.cvvexxx.frontend.client.keycloak.KeycloakRestClient;
import io.cvvexxx.frontend.dto.KeycloakTokenResponse;
import io.cvvexxx.frontend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakPasswordAuthenticationProvider implements AuthenticationProvider {

    private final KeycloakRestClient keycloakRestClient;
    private final JwtUtils jwtUtils;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        try {
            KeycloakTokenResponse tokenResponse = keycloakRestClient.login(username, password);

            JsonNode payloadNode = jwtUtils.parsePayload(tokenResponse.accessToken());
            UUID userId = jwtUtils.extractUserId(payloadNode);
            List<GrantedAuthority> authorities = jwtUtils.extractAuthorities(payloadNode);

            return new KeycloakJwtAuthenticationToken(
                    username,
                    userId,
                    tokenResponse.accessToken(),
                    tokenResponse.refreshToken(),
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
}