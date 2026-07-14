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

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtCookieFilter extends OncePerRequestFilter {

    private final RestClientUserRestClient restClient;
    private final Logger logger = LoggerFactory.getLogger(JwtCookieFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = getCookieValue(request, "JWT_TOKEN");
        String refreshToken = getCookieValue(request, "REFRESH_TOKEN");

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            boolean isAuthenticated = false;

            if (accessToken != null) {
                authenticateUser(accessToken);
                isAuthenticated = true;
            }

            if (!isAuthenticated && refreshToken != null) {
                try {
                    logger.info("Access token is missing/expired. Attempting to refresh using Refresh Token...");

                    JwtAuthenticationDto newTokens = restClient.refreshTokens(refreshToken);

                    ResponseCookie accessTokenCookie = ResponseCookie.from("JWT_TOKEN", newTokens.token())
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(15 * 60) //15 min
                            .sameSite("Lax")
                            .build();

                    ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", newTokens.refreshToken())
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(30 * 24 * 60 * 60) // 30 days
                            .sameSite("Lax")
                            .build();

                    response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
                    response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

                    authenticateUser(newTokens.token());

                } catch (Exception refreshEx) {
                    logger.error("Refresh token is also expired or invalid. User must log in again.", refreshEx);
                    clearCookies(response);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(String token) {
        UserDto userDto = restClient.getUserInfo(token);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDto,
                null,
                userDto.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void clearCookies(HttpServletResponse response) {
        ResponseCookie cleanAccess = ResponseCookie.from("JWT_TOKEN", "").path("/").maxAge(0).build();
        ResponseCookie cleanRefresh = ResponseCookie.from("REFRESH_TOKEN", "").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefresh.toString());
    }
}
