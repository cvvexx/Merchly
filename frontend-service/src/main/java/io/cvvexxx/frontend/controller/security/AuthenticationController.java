package io.cvvexxx.frontend.controller.security;


import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import io.cvvexxx.frontend.controller.security.payload.UserLoginPayload;
import io.cvvexxx.frontend.controller.security.payload.UserRegistrationPayload;
import io.cvvexxx.frontend.dto.JwtAuthenticationDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {

    private final RestClientUserRestClient restClient;

    @GetMapping("/registration")
    public String registrationPage(
            @RequestParam(value = "target", required = false) String target,
             Model model
    ) {
        model.addAttribute("target", target);


        return "security/registration";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "target", required = false) String target,
                            Model model) {
        if ("unauthorized".equals(error)) {
            model.addAttribute("message", "Сперва необходимо зарегистрироваться или войти в аккаунт!");
        }

        model.addAttribute("target", target);
        return "security/login";
    }

    @PostMapping("/do-register")
    public String registerUser(
            UserRegistrationPayload payload,
            @RequestParam(value = "target", required = false) String target
    ) {
        restClient.registerUser(payload.username(), payload.password());

        if (target != null && target.startsWith("/")) {
            return "redirect:/login?target=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
        }

        return "redirect:/login";
    }

    @PostMapping("/do-login")
    public String loginUser(
            UserLoginPayload payload,
            @RequestParam(value = "target", required = false) String target,
            HttpServletResponse response) {
        try {
            JwtAuthenticationDto jwtAuthenticationDto = restClient.checkUserAuth(payload.username(), payload.password());
            String accessToken = jwtAuthenticationDto.token();
            String refreshToken = jwtAuthenticationDto.refreshToken();

            ResponseCookie accessTokenCookie = ResponseCookie.from("JWT_TOKEN", accessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(15 * 60) // 15 min
                    .sameSite("Lax")
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
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
        } catch (Exception e) {
            return "redirect:/login?error";
        }
    }

    @PostMapping("/logout")//TODO(НЕ РАБОТАЕТ)
    public String logoutUser(HttpServletResponse response) {
        ResponseCookie jwtCookie = ResponseCookie.from("JWT_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return "";//TODO
    }
}
