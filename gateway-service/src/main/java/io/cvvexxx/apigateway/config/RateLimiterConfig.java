package io.cvvexxx.apigateway.config;

import io.cvvexxx.apigateway.security.ratelimit.RateLimitKeyResolver;
import io.cvvexxx.apigateway.security.ratelimit.RateLimitPolicy;
import io.cvvexxx.apigateway.security.ratelimit.RateLimitRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimitKeyResolver rateLimitKeyResolver(
            @Value("${merchly.ratelimit.trust-forwarded-for:false}") boolean trustForwardedFor,
            @Value("${merchly.ratelimit.internal-networks:127.0.0.1/32,::1/128,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}")
            String internalNetworks
    ) {
        return new RateLimitKeyResolver(trustForwardedFor, internalNetworks);
    }

    @Bean
    public RateLimitPolicy rateLimitPolicy(RateLimitKeyResolver rateLimitKeyResolver) {
        return new RateLimitPolicy(rateLimitKeyResolver, RateLimitRules.defaults());
    }
}
