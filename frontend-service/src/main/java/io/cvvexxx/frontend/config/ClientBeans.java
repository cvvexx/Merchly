package io.cvvexxx.frontend.config;

import io.cvvexxx.frontend.security.interceptor.TokenPropagationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class ClientBeans {

    @Bean
    public TokenPropagationInterceptor tokenPropagationInterceptor() {
        return new TokenPropagationInterceptor();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/error",
                "/.well-known/**"
        );
    }
}