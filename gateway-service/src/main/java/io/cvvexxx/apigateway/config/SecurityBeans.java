package io.cvvexxx.apigateway.config;

import io.cvvexxx.apigateway.client.KeycloakRestClient;
import io.cvvexxx.apigateway.security.JwtUtils;
import io.cvvexxx.apigateway.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.apigateway.security.KeycloakTokenRefreshFilter;
import io.cvvexxx.apigateway.security.TokenRelayFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityBeans {

    private static final String JSESSIONID = "JSESSIONID";

    private static final String[] PUBLIC_BROWSER_PATHS = {
            "/",
            "/login",
            "/do-login",
            "/registration",
            "/do-register",
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",
            "/error",
            "/error-403",
            "/fallback/**",
            "/actuator/health",
            "/actuator/health/**"
    };

    private final KeycloakRestClient keycloakRestClient;
    private final JwtUtils jwtUtils;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/users/me").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain browserSecurityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfRequestHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_BROWSER_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/catalogue/products/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/catalogue/products/*/edit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/catalogue/products/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication instanceof KeycloakJwtAuthenticationToken token) {
                                keycloakRestClient.logout(token.getRefreshToken());
                            }
                        })
                        .logoutSuccessUrl("/")
                        .deleteCookies(JSESSIONID)
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if ("/favicon.ico".equals(request.getRequestURI())) {
                                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                                return;
                            }

                            String targetUri = request.getRequestURI();
                            String queryString = request.getQueryString();
                            if (queryString != null) {
                                targetUri += "?" + queryString;
                            }

                            response.sendRedirect("/login?reason=unauthorized&target="
                                    + URLEncoder.encode(targetUri, StandardCharsets.UTF_8));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendRedirect("/error-403"))
                )
                .addFilterBefore(
                        new KeycloakTokenRefreshFilter(keycloakRestClient, jwtUtils),
                        AuthorizationFilter.class
                )
                .addFilterAfter(new TokenRelayFilter(), AuthorizationFilter.class)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> groups = extractGroups(jwt);
            List<String> realmRoles = extractRealmRoles(jwt);

            return Stream.concat(groups.stream(), realmRoles.stream())
                    .filter(role -> role != null && !role.isBlank())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .distinct()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });

        return converter;
    }

    private List<String> extractGroups(Jwt jwt) {
        return Optional.ofNullable(jwt.getClaimAsStringList("groups"))
                .orElseGet(List::of);
    }

    private List<String> extractRealmRoles(Jwt jwt) {
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
}
