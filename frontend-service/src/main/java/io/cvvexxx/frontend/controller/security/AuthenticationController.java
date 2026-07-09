package io.cvvexxx.frontend.controller.security;


import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import io.cvvexxx.frontend.controller.security.payload.UserLoginPayload;
import io.cvvexxx.frontend.controller.security.payload.UserRegistrationPayload;
import io.cvvexxx.frontend.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {

    private final RestClientUserRestClient restClient;

    @GetMapping("/registration")
    public String registrationPage() {
        return "security/registration";
    }

    @PostMapping("/do-register")
    public String registerUser(
        UserRegistrationPayload payload
    ) {
        UserDto userDto = restClient.registerUser(payload.username(), payload.password());

        return "redirect:/login";
    }


    @GetMapping("/login")
    public String loginPage() {
        return "security/login";
    }

    @PostMapping("/do-login") // Меняем адрес здесь!
    public String loginUser(UserLoginPayload payload, HttpServletRequest request) {

        try {
            // 1. Отправляем запрос на бэкенд
            UserDto userDto = restClient.checkUserAuth(payload.username(), payload.password());

            // 2. Создаем аутентификацию вручную
            // В userDto.roles() у тебя приходят роли с префиксом "ROLE_"
            var authorities = userDto.roles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDto.username(),
                    null,
                    authorities
            );

            // 3. Кладем ее в контекст Spring Security
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 4. Сохраняем контекст в HTTP-сессию фронтенда
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            return "catalogue/main/main_page";

        } catch (Exception e) {
            // Если бэкенд вернул ошибку (например, 401 Unauthorized), кидаем обратно на страницу логина
            return "redirect:/login?error";
        }
    }

}
