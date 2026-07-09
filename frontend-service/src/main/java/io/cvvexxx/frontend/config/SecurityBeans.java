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
        return http.authorizeHttpRequests(auth -> auth
                        // РАЗРЕШАЕМ доступ к странице логина и к нашему кастомному URL обработчику!
                        .requestMatchers("/login", "/do-login", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login") // Указываем, ГДЕ лежит наша страница логина
                        // ВАЖНО: Убираем loginProcessingUrl, если он там был!
                        // Не пишите сюда "/do-login", иначе Spring Security снова начнет его перехватывать
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
