package io.cvvexxx.frontend.config;

import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityBeans {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/registration",
                                "/do-register",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error",
                                "/error-403",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/catalogue/products/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/catalogue/products/*/edit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/catalogue/products/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())))
                .build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> keycloakJwtAuthenticationConverter() {
        return jwt -> new KeycloakJwtAuthenticationToken(
                resolveUsername(jwt),
                resolveUserId(jwt),
                jwt.getTokenValue(),
                extractAuthorities(jwt)
        );
    }

    private String resolveUsername(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return (username != null && !username.isBlank()) ? username : jwt.getSubject();
    }

    private UUID resolveUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private List<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        collectRoles(Optional.ofNullable(jwt.getClaimAsStringList("groups")).orElseGet(List::of), authorities);
        collectRoles(realmRoles(jwt), authorities);

        return authorities;
    }

    private List<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }

        return List.of();
    }

    private void collectRoles(List<String> roles, List<GrantedAuthority> target) {
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String authorityName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(authorityName);
            if (!target.contains(authority)) {
                target.add(authority);
            }
        }
    }
}