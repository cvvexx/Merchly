package io.cvvexxx.frontend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class SecurityBeans {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",        // Добавлено: страница логина
                                "/do-login",     // Добавлено: обработчик формы входа
                                "/registration", // Добавлено: страница регистрации
                                "/do-register",  // Добавлено: обработчик регистрации
                                "/css/**",
                                "/js/**",
                                "/favicon.ico",  // Добавлено: чтобы исключить редиректы из-за иконки
                                "/error",
                                "/error-403",
                                "/actuator/health/**",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/catalogue/products/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/catalogue/products/*/edit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/catalogue/products/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String targetUri = request.getRequestURI();
                            String queryString = request.getQueryString();

                            if (queryString != null) {
                                targetUri += "?" + queryString;
                            }

                            if ("/favicon.ico".equals(request.getRequestURI())) {
                                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                                return;
                            }

                            response.sendRedirect("/login?reason=unauthorized&target="
                                    + URLEncoder.encode(targetUri, StandardCharsets.UTF_8));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            request.getRequestDispatcher("/error-403").forward(request, response);
                        })
                )
                .build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    Map<String, Object> attributes = oidcUserAuthority.getAttributes();
                    if (attributes.containsKey("realm_access")) {
                        Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
                        if (realmAccess.containsKey("roles")) {
                            List<String> roles = (List<String>) realmAccess.get("roles");
                            roles.forEach(role -> {
                                String authorityName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                                mappedAuthorities.add(new SimpleGrantedAuthority(authorityName));
                            });
                        }
                    }
                } else {
                    mappedAuthorities.add(authority);
                }
            });

            return mappedAuthorities;
        };
    }
}