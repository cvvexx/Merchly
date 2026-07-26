package io.cvvexxx.frontend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cvvexxx.frontend.client.keycloak.KeycloakRestClient;
import io.cvvexxx.frontend.dto.KeycloakTokenResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

@RequiredArgsConstructor
@Slf4j
@Component
public class KeycloakTokenRefreshFilter extends OncePerRequestFilter {

    private final KeycloakRestClient keycloakClient;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication instanceof KeycloakJwtAuthenticationToken jwtAuth) {
            // Проверяем срок жизни токена
            if (isTokenExpired(jwtAuth.getCredentials().toString())) {
                log.info("Access token is expired. Refreshing session for user: {}", jwtAuth.getName());
                try {
                    // Обращаемся к Keycloak за новыми токенами
                    KeycloakTokenResponse refreshed = keycloakClient.refresh(jwtAuth.getRefreshToken());

                    // Создаем новый токен с обновленными данными
                    KeycloakJwtAuthenticationToken newToken = new KeycloakJwtAuthenticationToken(
                            jwtAuth.getName(),
                            refreshed.accessToken(),
                            refreshed.refreshToken(),
                            jwtAuth.getAuthorities()
                    );

                    // Обновляем контекст и сессию
                    context.setAuthentication(newToken);
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
                    }
                    log.info("Token successfully refreshed!");

                } catch (Exception e) {
                    log.error("Failed to refresh token, forcing logout", e);
                    SecurityContextHolder.clearContext();
                    HttpSession session = request.getSession(false);
                    if (session != null) session.invalidate();

                    response.sendRedirect("/login?error=session_expired");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isTokenExpired(String accessToken) {
        try {
            String[] chunks = accessToken.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(chunks[1]));
            JsonNode rootNode = objectMapper.readTree(payloadJson);
            long exp = rootNode.path("exp").asLong();
            long currentTime = System.currentTimeMillis() / 1000;

            return exp <= (currentTime + 10);
        } catch (Exception e) {
            return true;
        }
    }
}
