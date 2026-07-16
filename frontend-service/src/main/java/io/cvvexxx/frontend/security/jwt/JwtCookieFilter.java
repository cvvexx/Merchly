package io.cvvexxx.frontend.security.jwt;

import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import io.cvvexxx.frontend.dto.JwtAuthenticationDto;
import io.cvvexxx.frontend.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private static final String JWT_COOKIE_NAME = "JWT_TOKEN";
    private static final String REFRESH_COOKIE_NAME = "REFRESH_TOKEN";

    private final RestClientUserRestClient restClient;
    private final Logger logger = LoggerFactory.getLogger(JwtCookieFilter.class);

    @Value("${jwt.token.secret}")
    private String jwtSecret;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64URL.decode(jwtSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = getCookieValue(request, JWT_COOKIE_NAME);
        String refreshToken = getCookieValue(request, REFRESH_COOKIE_NAME);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            boolean isAuthenticated = false;

            // 1. Попытка локальной валидации Access Token
            if (accessToken != null) {
                try {
                    Claims claims = parseToken(accessToken);
                    authenticateUserFromClaims(claims, accessToken);
                    isAuthenticated = true;
                } catch (ExpiredJwtException e) {
                    logger.warn("Access token has expired.");
                } catch (Exception e) {
                    logger.warn("Access token invalid. Message: {}", e.getMessage());
                }
            }

            // 2. Если Access Token невалиден/истек, но есть Refresh Token — обновляем сессию (Сетевой запрос)
            if (!isAuthenticated && refreshToken != null) {
                try {
                    logger.info("Attempting to refresh session using Refresh Token...");
                    JwtAuthenticationDto newTokens = restClient.refreshTokens(refreshToken);

                    // Записываем новые куки клиенту
                    writeTokenCookies(response, newTokens);

                    // Локально парсим уже НОВЫЙ accessToken и авторизуем
                    Claims claims = parseToken(newTokens.token());
                    authenticateUserFromClaims(claims, newTokens.token());

                    logger.info("Session successfully refreshed locally.");
                } catch (Exception refreshEx) {
                    logger.error("Refresh token is also expired or invalid. User must log in again.");
                    clearCookies(response);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    // Локальный парсинг JWT без походов в другие сервисы
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Сборка авторизации на основе расшифрованных клеймов (Claims)
    private void authenticateUserFromClaims(Claims claims, String token) {
        String username = claims.get("username", String.class);

        // Достаем роли (убедись, что твой auth-сервис упаковывает их в JWT как List)
        @SuppressWarnings("unchecked")
        List<String> rolesList = claims.get("roles", List.class);

        // 2. Преобразуем List в Set (если в UserDto требуется именно Set)
        Set<String> roles = rolesList != null ? Set.copyOf(rolesList) : Set.of();

        int id = claims.get("id", Integer.class);

        // Собираем DTO "на лету" из токена
        UserDto userDto = new UserDto(id, username, roles);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDto,
                token, // Передаем сырой токен как Credentials
                roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return (cookie != null) ? cookie.getValue() : null;
    }

    private void writeTokenCookies(HttpServletResponse response, JwtAuthenticationDto tokens) {
        ResponseCookie accessTokenCookie = ResponseCookie.from(JWT_COOKIE_NAME, tokens.token())
                .httpOnly(true)
                .secure(false) // Сделай true на продакшене (HTTPS)
                .path("/")
                .maxAge(15 * 60)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, tokens.refreshToken())
                .httpOnly(true)
                .secure(false) // Сделай true на продакшене (HTTPS)
                .path("/")
                .maxAge(30L * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    private void clearCookies(HttpServletResponse response) {
        ResponseCookie cleanAccess = ResponseCookie.from(JWT_COOKIE_NAME, "").path("/").maxAge(0).build();
        ResponseCookie cleanRefresh = ResponseCookie.from(REFRESH_COOKIE_NAME, "").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefresh.toString());
    }
}