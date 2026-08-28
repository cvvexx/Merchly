package io.cvvexxx.apigateway.security.ratelimit;

import io.cvvexxx.apigateway.security.KeycloakJwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private static final String EXTERNAL_IP = "203.0.113.10";
    private static final String INTERNAL_IP = "172.18.0.7";

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        var keyResolver = new RateLimitKeyResolver(false, "127.0.0.1/32,172.16.0.0/12");
        filter = new RateLimitFilter(new RateLimitPolicy(keyResolver, RateLimitRules.defaults()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String method, String uri, String remoteAddr) {
        var request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private void authenticate(UUID userId) {
        var token = new KeycloakJwtAuthenticationToken(
                "user-" + userId, userId, "access-token", "refresh-token", List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private void drain(int times, String method, String uri, String remoteAddr, String login) {
        for (int attempt = 0; attempt < times; attempt++) {
            var request = request(method, uri, remoteAddr);
            if (login != null) {
                request.setParameter("login", login);
            }
            try {
                filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    @Test
    @DisplayName("пропускает запрос, пока лимит не исчерпан")
    void doFilter_WhenUnderLimit_ShouldPassRequestThrough() throws Exception {
        var chain = mock(FilterChain.class);
        var request = request("POST", "/do-login", EXTERNAL_IP);
        request.setParameter("login", "johndoe");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("исчерпав лимит на /do-login, редиректит на /login?error=rate_limited")
    void doFilter_WhenLoginLimitExceeded_ShouldRedirectToLoginPage() throws Exception {
        drain(5, "POST", "/do-login", EXTERNAL_IP, "johndoe");

        var chain = mock(FilterChain.class);
        var request = request("POST", "/do-login", EXTERNAL_IP);
        request.setParameter("login", "johndoe");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(302, response.getStatus());
        assertEquals("/login?error=rate_limited", response.getRedirectedUrl());
        assertNotNull(response.getHeader("Retry-After"));
    }

    @Test
    @DisplayName("лимит по аккаунту блокирует подбор одного логина с разных адресов")
    void doFilter_WhenSameAccountFromDifferentIps_ShouldStillBlock() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            var request = request("POST", "/do-login", "203.0.113." + attempt);
            request.setParameter("login", "johndoe");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        var chain = mock(FilterChain.class);
        var request = request("POST", "/do-login", "203.0.113.200");
        request.setParameter("login", "johndoe");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(302, response.getStatus());
    }

    @Test
    @DisplayName("исчерпав лимит регистрации, редиректит на /registration?error=rate_limited с target")
    void doFilter_WhenRegistrationLimitExceeded_ShouldRedirectPreservingTarget() throws Exception {
        drain(3, "POST", "/do-register", EXTERNAL_IP, null);

        var request = request("POST", "/do-register", EXTERNAL_IP);
        request.setParameter("target", "/orders?x=y");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("/registration?error=rate_limited&target=%2Forders%3Fx%3Dy", response.getRedirectedUrl());
    }

    @Test
    @DisplayName("для API отдаёт 429 с Retry-After, а не редирект")
    void doFilter_WhenApiLimitExceeded_ShouldReturnTooManyRequests() throws Exception {
        drain(120, "GET", "/api/products", EXTERNAL_IP, null);

        var chain = mock(FilterChain.class);
        var request = request("GET", "/api/products", EXTERNAL_IP);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("too_many_requests"));
        assertNotNull(response.getHeader("Retry-After"));
    }

    @Test
    @DisplayName("не лимитирует по IP запросы с внутренней сети: фронт ходит в API через шлюз")
    void doFilter_WhenAnonymousRequestFromInternalNetwork_ShouldNotConsumeIpBucket() throws Exception {
        drain(300, "GET", "/api/products", INTERNAL_IP, null);

        var chain = mock(FilterChain.class);
        var request = request("GET", "/api/products", INTERNAL_IP);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("аутентифицированные запросы считаются по пользователю, а не по адресу фронта")
    void doFilter_WhenAuthenticated_ShouldKeyByUserAcrossIps() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        for (int attempt = 0; attempt < 10; attempt++) {
            filter.doFilter(
                    request("POST", "/api/orders/create", INTERNAL_IP),
                    new MockHttpServletResponse(),
                    new MockFilterChain()
            );
        }

        var chain = mock(FilterChain.class);
        var response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/api/orders/create", EXTERNAL_IP), response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(429, response.getStatus());
    }

    @Test
    @DisplayName("лимит одного пользователя не задевает другого")
    void doFilter_WhenDifferentUsers_ShouldNotShareBucket() throws Exception {
        authenticate(UUID.randomUUID());
        for (int attempt = 0; attempt < 10; attempt++) {
            filter.doFilter(
                    request("POST", "/api/orders/create", INTERNAL_IP),
                    new MockHttpServletResponse(),
                    new MockFilterChain()
            );
        }

        SecurityContextHolder.clearContext();
        authenticate(UUID.randomUUID());

        var chain = mock(FilterChain.class);
        var request = request("POST", "/api/orders/create", INTERNAL_IP);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("создание отзыва и пакетная статистика считаются отдельно")
    void doFilter_WhenReviewCreateDrained_ShouldNotBlockStats() throws Exception {
        authenticate(UUID.randomUUID());
        for (int attempt = 0; attempt < 10; attempt++) {
            filter.doFilter(
                    request("POST", "/api/reviews/products", INTERNAL_IP),
                    new MockHttpServletResponse(),
                    new MockFilterChain()
            );
        }

        var blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/reviews/products", INTERNAL_IP), blockedResponse, new MockFilterChain());
        assertEquals(429, blockedResponse.getStatus());

        var chain = mock(FilterChain.class);
        var statsRequest = request("POST", "/api/reviews/products/stats", INTERNAL_IP);
        var statsResponse = new MockHttpServletResponse();

        filter.doFilter(statsRequest, statsResponse, chain);

        verify(chain).doFilter(statsRequest, statsResponse);
    }

    @Test
    @DisplayName("не трогает пути без правил")
    void doFilter_WhenNoRuleMatches_ShouldAlwaysPassThrough() throws Exception {
        drain(500, "GET", "/css/app.css", EXTERNAL_IP, null);

        var chain = mock(FilterChain.class);
        var request = request("GET", "/css/app.css", EXTERNAL_IP);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(response.getRedirectedUrl());
    }

    @Test
    @DisplayName("по умолчанию игнорирует X-Forwarded-For: иначе лимит обходится подделкой заголовка")
    void doFilter_WhenForwardedForNotTrusted_ShouldKeyByRemoteAddress() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            var request = request("POST", "/do-register", EXTERNAL_IP);
            request.addHeader("X-Forwarded-For", "198.51.100." + attempt);
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        var chain = mock(FilterChain.class);
        var request = request("POST", "/do-register", EXTERNAL_IP);
        request.addHeader("X-Forwarded-For", "198.51.100.99");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(302, response.getStatus());
    }
}
