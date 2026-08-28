package io.cvvexxx.apigateway.security.ratelimit;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public record RateLimitRule(
        String name,
        Set<HttpMethod> methods,
        PathPattern pathPattern,
        RateLimitKeyType keyType,
        int capacity,
        Duration window
) {

    private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;

    public static RateLimitRule of(
            String name,
            String path,
            RateLimitKeyType keyType,
            int capacity,
            Duration window,
            HttpMethod... methods
    ) {
        Set<HttpMethod> methodSet = methods.length == 0
                ? Set.of()
                : Set.copyOf(Arrays.asList(methods));

        return new RateLimitRule(name, methodSet, PARSER.parse(path), keyType, capacity, window);
    }

    public boolean matches(HttpMethod method, PathContainer path) {
        return (methods.isEmpty() || methods.contains(method)) && pathPattern.matches(path);
    }
}
