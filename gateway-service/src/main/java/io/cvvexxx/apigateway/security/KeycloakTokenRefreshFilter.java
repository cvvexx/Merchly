package io.cvvexxx.apigateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import io.cvvexxx.apigateway.client.KeycloakRestClient;
import io.cvvexxx.apigateway.dto.KeycloakTokenResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class KeycloakTokenRefreshFilter extends OncePerRequestFilter {

    private static final long EXPIRY_BUFFER_SECONDS = 10;

    private final KeycloakRestClient keycloakClient;
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof KeycloakJwtAuthenticationToken jwtAuth)
                || !jwtUtils.isTokenExpired(jwtAuth.getAccessToken(), EXPIRY_BUFFER_SECONDS)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("Access token is expired. Refreshing session for user: {}", jwtAuth.getName());
        try {
            KeycloakTokenResponse refreshed = keycloakClient.refresh(jwtAuth.getRefreshToken());

            JsonNode payloadNode = jwtUtils.parsePayload(refreshed.accessToken());

            UUID userId = jwtUtils.extractUserId(payloadNode);
            if (userId == null) userId = jwtAuth.getUserId();

            List<GrantedAuthority> authorities = jwtUtils.extractAuthorities(payloadNode);
            if (authorities.isEmpty()) authorities = new ArrayList<>(jwtAuth.getAuthorities());

            KeycloakJwtAuthenticationToken newToken = new KeycloakJwtAuthenticationToken(
                    jwtAuth.getName(),
                    userId,
                    refreshed.accessToken(),
                    refreshed.refreshToken(),
                    authorities
            );

            context.setAuthentication(newToken);
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            }
            log.info("Token successfully refreshed for user: {}", jwtAuth.getName());

        } catch (Exception e) {
            log.error("Failed to refresh token, forcing logout", e);
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();

            response.sendRedirect("/login?error=session_expired");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
