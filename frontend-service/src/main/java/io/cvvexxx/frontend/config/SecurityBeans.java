package io.cvvexxx.frontend.config;


import io.cvvexxx.frontend.security.jwt.JwtCookieFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class SecurityBeans {

    private final JwtCookieFilter jwtCookieFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::none))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/",
                                "/login", "/do-login",
                                "/registration", "/do-register",
                                "/logout",
                                "/css/**", "/js/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/catalogue/products/**")
                        .hasRole("ADMIN")//TODO(мб перенести обработку всего этого на бэк)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String targetUri = request.getRequestURI();
                            String queryString = request.getQueryString();

                            if (queryString != null) {
                                targetUri += "?" + queryString;
                            }

                            response.sendRedirect("/login?error=unauthorized&target="
                                    + URLEncoder.encode(targetUri, StandardCharsets.UTF_8));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                            request.getRequestDispatcher("/error-403").forward(request, response);
                        })
                )
                .build();
    }


}
