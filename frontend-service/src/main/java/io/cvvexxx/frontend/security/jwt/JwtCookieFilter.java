package io.cvvexxx.frontend.security.jwt;

import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import io.cvvexxx.frontend.dto.JwtAuthenticationDto;
import io.cvvexxx.frontend.dto.UserDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private final RestClientUserRestClient restClient;
    private final Logger logger = LoggerFactory.getLogger(JwtCookieFilter.class);

    private static final String JWT_COOKIE_NAME = "JWT_TOKEN";
    private static final String REFRESH_COOKIE_NAME = "REFRESH_TOKEN";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Быстро и чисто достаем куки с помощью WebUtils
        String accessToken = getCookieValue(request, JWT_COOKIE_NAME);
        String refreshToken = getCookieValue(request, REFRESH_COOKIE_NAME);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            boolean isAuthenticated = false;

            // 2. Пытаемся авторизоваться по текущему Access Token
            if (accessToken != null) {
                try {
                    authenticateUser(accessToken);
                    isAuthenticated = true;
                } catch (Exception e) {
                    logger.warn("Access token invalid or expired. Message: {}", e.getMessage());
                    // Не падаем! Даем коду пройти дальше к попытке обновления через Refresh Token
                }
            }

            // 3. Если авторизоваться не удалось, но есть Refresh Token — обновляем сессию
            if (!isAuthenticated && refreshToken != null) {
                try {
                    logger.info("Attempting to refresh session using Refresh Token...");
                    JwtAuthenticationDto newTokens = restClient.refreshTokens(refreshToken);

                    // Сохраняем новые куки в ответе
                    writeTokenCookies(response, newTokens);

                    // КРИТИЧЕСКИ ВАЖНО: Сразу авторизуем текущий запрос новым токеном!
                    authenticateUser(newTokens.token());
                    logger.info("Session successfully refreshed and authenticated for current request.");

                } catch (Exception refreshEx) {
                    logger.error("Refresh token is also expired or invalid. User must log in again.", refreshEx);
                    clearCookies(response);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(String token) {
        UserDto userDto = restClient.getUserInfo();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDto,
                token,
                userDto.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
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
                .secure(false) // Поставь true, если тестируешь под HTTPS в продакшене
                .path("/")
                .maxAge(15 * 60) // 15 мин
                .sameSite("Lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, tokens.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(30L * 24 * 60 * 60) // 30 дней
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