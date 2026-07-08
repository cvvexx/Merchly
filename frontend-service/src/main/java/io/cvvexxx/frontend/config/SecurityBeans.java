package io.cvvexxx.frontend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityBeans {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        configurer ->
                                configurer
                                        .requestMatchers("/login", "/logout", "/css/**", "/js/**")
                                        .permitAll()
                                        .anyRequest().authenticated()
                )
                .formLogin(
                        configurer -> configurer
                                .loginPage("/login")
                                .defaultSuccessUrl("/", true)
                                .failureUrl("/login")
                                .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true) // Инвалидируем сессию
                        .deleteCookies("JSESSIONID") // Чистим куки
                        .permitAll()
                )
                .build();
    }



}
