package io.cvvexxx.apigateway.security.ratelimit;

import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.util.List;

import static io.cvvexxx.apigateway.security.ratelimit.RateLimitKeyType.*;

public final class RateLimitRules {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private RateLimitRules() {
    }

    public static List<RateLimitRule> defaults() {
        return List.of(
                RateLimitRule.of("login-ip", "/do-login", IP, 10, FIVE_MINUTES, HttpMethod.POST),
                RateLimitRule.of("login-account", "/do-login", FORM_LOGIN, 5, FIFTEEN_MINUTES, HttpMethod.POST),
                RateLimitRule.of("register-browser", "/do-register", IP, 3, ONE_HOUR, HttpMethod.POST),
                RateLimitRule.of("register-api", "/api/users/register", IP, 3, ONE_HOUR, HttpMethod.POST),

                RateLimitRule.of("products-read", "/api/products/**", IP, 120, ONE_MINUTE, HttpMethod.GET),
                RateLimitRule.of("products-create", "/api/products", USER, 20, ONE_HOUR, HttpMethod.POST),
                RateLimitRule.of("products-write", "/api/products/*", USER, 60, ONE_MINUTE,
                        HttpMethod.PATCH, HttpMethod.DELETE),

                RateLimitRule.of("reviews-read", "/api/reviews/**", IP, 120, ONE_MINUTE, HttpMethod.GET),
                RateLimitRule.of("reviews-create", "/api/reviews/products", USER, 10, TEN_MINUTES, HttpMethod.POST),
                RateLimitRule.of("reviews-stats", "/api/reviews/products/stats", USER, 60, ONE_MINUTE, HttpMethod.POST),
                RateLimitRule.of("reviews-update", "/api/reviews/products", USER, 20, TEN_MINUTES, HttpMethod.PATCH),
                RateLimitRule.of("reviews-delete", "/api/reviews/products/*", USER, 20, TEN_MINUTES, HttpMethod.DELETE),

                RateLimitRule.of("orders-create", "/api/orders/create", USER, 10, ONE_MINUTE, HttpMethod.POST),
                RateLimitRule.of("orders-confirm", "/api/orders/*/confirm", USER, 20, ONE_MINUTE, HttpMethod.POST),
                RateLimitRule.of("orders-cancel", "/api/orders/*/cancel", USER, 20, ONE_MINUTE, HttpMethod.POST),
                RateLimitRule.of("orders-read", "/api/orders/**", USER, 120, ONE_MINUTE, HttpMethod.GET),

                RateLimitRule.of("profile-edit-api", "/api/users/edit", USER, 5, TEN_MINUTES, HttpMethod.POST),
                RateLimitRule.of("cart-write-api", "/api/users/cart/**", USER, 60, ONE_MINUTE,
                        HttpMethod.POST, HttpMethod.DELETE),

                RateLimitRule.of("catalogue-write", "/catalogue/products/**", USER, 30, ONE_HOUR, HttpMethod.POST),
                RateLimitRule.of("catalogue-reviews", "/catalogue/reviews/**", USER, 20, TEN_MINUTES,
                        HttpMethod.POST, HttpMethod.PATCH, HttpMethod.DELETE),
                RateLimitRule.of("orders-create-browser", "/orders/create", USER, 10, ONE_MINUTE, HttpMethod.POST),
                RateLimitRule.of("orders-transition-browser", "/orders/*/*", USER, 20, ONE_MINUTE, HttpMethod.POST),
                RateLimitRule.of("cart-write-browser", "/cart/**", USER, 60, ONE_MINUTE,
                        HttpMethod.POST, HttpMethod.DELETE),
                RateLimitRule.of("profile-edit-browser", "/profile/edit", USER, 5, TEN_MINUTES, HttpMethod.POST)
        );
    }
}
