package io.cvvexxx.apigateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.cvvexxx.apigateway.client.KeycloakRestClient;
import io.cvvexxx.apigateway.dto.KeycloakTokenResponse;
import io.cvvexxx.apigateway.security.JwtUtils;
import io.cvvexxx.apigateway.security.KeycloakJwtAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final KeycloakRestClient keycloakClient;
    private final JwtUtils jwtUtils;

    @GetMapping("/login")
    public String getLoginPage(
            Model model,
            @RequestParam(name = "target", required = false) String target
    ) {
        model.addAttribute("target", target);
        return "login";
    }

    @PostMapping("/do-login")
    public String loginUser(
            @RequestParam("login") String login,
            @RequestParam("password") String password,
            @RequestParam(name = "target", required = false) String target,
            HttpServletRequest request
    ) {
        try {
            KeycloakTokenResponse tokenResponse = keycloakClient.login(login, password);
            authenticateInSession(login, tokenResponse, request);

            return (target != null && !target.isBlank())
                    ? "redirect:" + target
                    : "redirect:/catalogue/products/list";
        } catch (Exception e) {
            log.error("Ошибка при входе пользователя {}: {}", login, e.getMessage());

            String redirectUrl = "redirect:/login?error=true";
            if (target != null && !target.isBlank()) {
                redirectUrl += "&target=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
            }
            return redirectUrl;
        }
    }

    private void authenticateInSession(
            String username,
            KeycloakTokenResponse tokenResponse,
            HttpServletRequest request
    ) {
        JsonNode payloadNode = jwtUtils.parsePayload(tokenResponse.accessToken());
        UUID userId = jwtUtils.extractUserId(payloadNode);
        List<GrantedAuthority> authorities = jwtUtils.extractAuthorities(payloadNode);

        KeycloakJwtAuthenticationToken authToken = new KeycloakJwtAuthenticationToken(
                username,
                userId,
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                authorities
        );

        HttpSession previousSession = request.getSession(false);
        if (previousSession != null) {
            previousSession.invalidate();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        log.info("Пользователь {} аутентифицирован на шлюзе", username);
    }
}
