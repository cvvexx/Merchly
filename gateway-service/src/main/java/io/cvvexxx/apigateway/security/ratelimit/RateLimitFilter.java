package io.cvvexxx.apigateway.security.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String TARGET_PARAM = "target";
    private static final Map<String, String> FORM_REDIRECTS = Map.of(
            "/do-login", "/login",
            "/do-register", "/registration"
    );

    private final RateLimitPolicy policy;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<ConsumptionProbe> rejection = policy.check(request);

        if (rejection.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        rejectRequest(request, response, rejection.get());
    }

    private void rejectRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            ConsumptionProbe probe
    ) throws IOException {
        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        String redirectBase = FORM_REDIRECTS.get(request.getRequestURI());
        if (redirectBase != null) {
            response.sendRedirect(redirectBase + "?error=rate_limited" + encodedTarget(request));
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"error\":\"too_many_requests\",\"retryAfterSeconds\":" + retryAfterSeconds + "}"
        );
    }

    private String encodedTarget(HttpServletRequest request) {
        String target = request.getParameter(TARGET_PARAM);
        if (target == null || target.isBlank()) {
            return "";
        }

        return "&" + TARGET_PARAM + "=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
    }
}
