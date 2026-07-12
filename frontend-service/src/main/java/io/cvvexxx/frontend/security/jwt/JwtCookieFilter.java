package io.cvvexxx.frontend.security.jwt;

import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import io.cvvexxx.frontend.dto.UserDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        try {
            String token = null;
            if (request.getCookies() != null) {
                token = Arrays.stream(request.getCookies())
                        .filter(cookie -> "JWT_TOKEN".equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }

            logger.info("doFilterIternal: token: {}", token);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                logger.info("doFilterIternal: Authentication: {}", SecurityContextHolder.getContext()
                        .getAuthentication());

                UserDto userDto = restClient.getUserInfo(token);

                logger.info("UserDto: {}", userDto);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDto,
                                null,
                                userDto.roles().stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .toList()
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.info("doFilterIternal: Authentication after auth: {}", SecurityContextHolder.getContext()
                        .getAuthentication());
            }
        } catch (Exception e) {
            //TODO(Добавить логику refresh токена)
        }

        filterChain.doFilter(request, response);
    }
}
