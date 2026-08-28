package io.cvvexxx.apigateway.security.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
public class RateLimitPolicy {

    private static final String LOGIN_PARAM = "login";

    private final RateLimitKeyResolver keyResolver;
    private final List<Entry> entries;

    public RateLimitPolicy(RateLimitKeyResolver keyResolver, List<RateLimitRule> rules) {
        this.keyResolver = keyResolver;
        this.entries = rules.stream()
                .map(rule -> new Entry(rule, new RateLimitBuckets(rule.capacity(), rule.window())))
                .toList();
    }

    public Optional<ConsumptionProbe> check(HttpServletRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        PathContainer path = PathContainer.parsePath(request.getRequestURI());

        String clientIp = keyResolver.clientIp(request);
        boolean internal = keyResolver.isInternal(clientIp);
        String principal = keyResolver.principal().orElse(null);

        for (Entry entry : entries) {
            if (!entry.rule().matches(method, path)) {
                continue;
            }

            String key = resolveKey(entry.rule().keyType(), request, clientIp, principal, internal);
            if (key == null) {
                continue;
            }

            ConsumptionProbe probe = entry.buckets().tryConsume(key);
            if (!probe.isConsumed()) {
                log.warn("Превышен лимит {} для ключа {} (адрес {})", entry.rule().name(), key, clientIp);
                return Optional.of(probe);
            }
        }

        return Optional.empty();
    }

    private String resolveKey(
            RateLimitKeyType keyType,
            HttpServletRequest request,
            String clientIp,
            String principal,
            boolean internal
    ) {
        return switch (keyType) {
            case IP -> internal ? null : "ip:" + clientIp;
            case USER -> {
                if (principal != null) {
                    yield "user:" + principal;
                }
                yield internal ? null : "ip:" + clientIp;
            }
            case FORM_LOGIN -> {
                String login = request.getParameter(LOGIN_PARAM);
                yield login == null || login.isBlank()
                        ? null
                        : "login:" + login.trim().toLowerCase(Locale.ROOT);
            }
        };
    }

    private record Entry(RateLimitRule rule, RateLimitBuckets buckets) {
    }
}
