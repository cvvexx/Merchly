package io.cvvexxx.frontend.controller.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cvvexxx.frontend.client.keycloak.KeycloakRestClient;
import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.CreatedUserDto;
import io.cvvexxx.frontend.dto.KeycloakTokenResponse;
import io.cvvexxx.frontend.dto.LoginUserDto;
import io.cvvexxx.frontend.dto.NewUserDto;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final RestClientUserPublicRestClient userRestClient;
    private final KeycloakRestClient keycloakClient;
    private final ObjectMapper objectMapper;

    @GetMapping("/login")
    public String getLoginPage(
            Model model,
            @RequestParam(name = "target", required = false) String target
    ) {
        model.addAttribute("target", target);
        return "security/login";
    }

    @GetMapping("/registration")
    public String getRegistrationPage(
            Model model,
            @RequestParam(name = "target", required = false) String target
    ) {

        model.addAttribute("target", target);
        return "security/registration";
    }

    @PostMapping("/do-register")
    public String registerUser(
            NewUserDto newUserDto,
            @RequestParam(name = "target", required = false) String target,
            Model model
    ) {
        try {
            CreatedUserDto createdUserDto = userRestClient.registerUser(newUserDto);
            log.info("Successfully registered user: {}", createdUserDto.username());
            model.addAttribute("target", target);
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Ошибка при регистрации: {}", e.getMessage());
            return "redirect:/registration?error=invalid_data";
        }
    }

    @PostMapping("/do-login")
    public String loginUser(
            LoginUserDto loginUserDto,
            @RequestParam(name = "target", required = false) String target,
            HttpServletRequest request
    ) {
        try {
            KeycloakTokenResponse tokenResponse = keycloakClient.login(loginUserDto.login(), loginUserDto.password());
            authenticateUserInSession(
                    loginUserDto.login(),
                    tokenResponse.accessToken(),
                    tokenResponse.refreshToken(),
                    request
            );
            return (target != null && !target.isBlank()) ? "redirect:" + target : "redirect:/catalogue/products/list";

        } catch (Exception e) {
            log.error("Ошибка при логине: {}", e.getMessage());
            return "redirect:/login?error=unauthorized";
        }
    }

    private void authenticateUserInSession(
            String username,
            String accessToken,
            String refreshToken,
            HttpServletRequest request
    ) {
        log.info("Trying to authenticate user: {}", username);
        List<GrantedAuthority> authorities = extractAuthorities(accessToken);
        log.info("Authorities: {}", authorities);
        KeycloakJwtAuthenticationToken authToken = new KeycloakJwtAuthenticationToken(
                username,
                accessToken,
                refreshToken,
                authorities
        );
        log.info("Authentication token: {}", authToken);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        log.info("Authentication: {}", context.getAuthentication());
        // Сохраняем SecurityContext в HTTP сессию
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private List<GrantedAuthority> extractAuthorities(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return Collections.emptyList();
        try {
            String[] chunks = accessToken.split("\\.");
            if (chunks.length < 2) return Collections.emptyList();

            String payloadJson = new String(Base64.getUrlDecoder().decode(chunks[1]));
            JsonNode rootNode = objectMapper.readTree(payloadJson);
            JsonNode realmAccessRoles = rootNode.path("realm_access").path("roles");

            List<GrantedAuthority> authorities = new ArrayList<>();
            if (realmAccessRoles.isArray()) {
                for (JsonNode roleNode : realmAccessRoles) {
                    String roleName = roleNode.asText();
                    String authorityName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    authorities.add(new SimpleGrantedAuthority(authorityName));
                }
            }
            return authorities;
        } catch (Exception e) {
            log.error("Failed to parse roles from token", e);
            return Collections.emptyList();
        }
    }
}