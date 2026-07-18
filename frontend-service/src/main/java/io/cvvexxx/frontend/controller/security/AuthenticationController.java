package io.cvvexxx.frontend.controller.security;


import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import io.cvvexxx.frontend.controller.security.payload.NewUserPayload;
import io.cvvexxx.frontend.controller.security.payload.UserLoginPayload;
import io.cvvexxx.frontend.dto.JwtAuthenticationDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {

    private final RestClientUserRestClient restClient;

    private static final String ACCESS_TOKEN_COOKIE_NAME = "JWT_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

    @GetMapping("/registration")
    public String registrationPage(
            @RequestParam(value = "target", required = false) String target,
            Model model
    ) {
        model.addAttribute("target", target);


        return "security/registration";
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "target", required = false) String target,
            Model model
    ) {
        if ("unauthorized".equals(error)) {
            model.addAttribute("message", "Сперва необходимо зарегистрироваться или войти в аккаунт!");
        }

        model.addAttribute("target", target);
        return "security/login";
    }

    @PostMapping("/do-register")
    public String registerUser(
            NewUserPayload payload,
            @RequestParam(value = "target", required = false) String target,
            Model model
    ) {
        try {
            restClient.registerUser(payload);

            if (target != null && target.startsWith("/")) {
                return "redirect:/login?target=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
            }

            return "redirect:/login";
        } catch (BadRequestException exception) {
            model.addAttribute("payload", payload);
            model.addAttribute("errors", exception.getErrors());
            model.addAttribute("target", target);
            return "security/registration";
        }

    }

    @PostMapping("/do-login")
    public String loginUser(
            UserLoginPayload payload,
            @RequestParam(value = "target", required = false) String target,
            HttpServletResponse response,
            Model model) {
        try {
            JwtAuthenticationDto jwtAuthenticationDto = restClient.checkUserAuth(payload);
            String accessToken = jwtAuthenticationDto.token();
            String refreshToken = jwtAuthenticationDto.refreshToken();

            ResponseCookie accessTokenCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(15 * 60) // 15 min
                    .sameSite("Lax")
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(30 * 24 * 60 * 60) // 30 days
                    .sameSite("Lax")
                    .build();


            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            if (target != null && target.startsWith("/")) {
                return "redirect:" + target;
            }

            return "redirect:/";
        } catch (HttpClientErrorException.Unauthorized exception) {
            // Ловим именно 401 Unauthorized от бэкенда (неверные логин/пароль)
            model.addAttribute("payload", payload);
            model.addAttribute("errors", java.util.List.of("Неверный логин или пароль"));
            model.addAttribute("target", target);
            return "security/login"; // Возвращаем на форму входа
        } catch (BadRequestException exception) {
            model.addAttribute("payload", payload);
            model.addAttribute("errors", exception.getErrors());
            model.addAttribute("target", target);
            return "security/login";
        }
    }

    @PostMapping("/logout")
    public String logoutUser(HttpServletResponse response) {
        ResponseCookie jwtCookie = ResponseCookie
                .from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return "redirect:/login?logout";
    }

    @RequestMapping("error-403")
    public String accessDenied() {
        return "error/403";
    }

}
