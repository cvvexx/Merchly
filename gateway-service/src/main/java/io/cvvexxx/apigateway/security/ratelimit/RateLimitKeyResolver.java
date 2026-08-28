package io.cvvexxx.apigateway.security.ratelimit;

import io.cvvexxx.apigateway.security.KeycloakJwtAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
public class RateLimitKeyResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final boolean trustForwardedFor;
    private final List<IpAddressMatcher> internalMatchers;

    public RateLimitKeyResolver(boolean trustForwardedFor, String internalNetworks) {
        this.trustForwardedFor = trustForwardedFor;
        this.internalMatchers = Arrays.stream(internalNetworks.split(","))
                .map(String::trim)
                .filter(network -> !network.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();
    }

    public String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    public boolean isInternal(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        return internalMatchers.stream().anyMatch(matcher -> safeMatches(matcher, ip));
    }

    public Optional<String> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication instanceof KeycloakJwtAuthenticationToken token && token.getUserId() != null) {
            return Optional.of(token.getUserId().toString());
        }

        if (authentication instanceof JwtAuthenticationToken token) {
            return Optional.ofNullable(token.getToken())
                    .map(Jwt::getSubject)
                    .filter(subject -> !subject.isBlank());
        }

        return Optional.empty();
    }

    private boolean safeMatches(IpAddressMatcher matcher, String ip) {
        try {
            return matcher.matches(ip);
        } catch (IllegalArgumentException exception) {
            log.debug("Не удалось сопоставить адрес {} с внутренней сетью", ip);
            return false;
        }
    }
}
