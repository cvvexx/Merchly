package io.cvvexxx.frontend.controller.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cvvexxx.frontend.client.keycloak.KeycloakRestClient;
import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.keycloak.KeycloakTokenResponse;
import io.cvvexxx.frontend.dto.user.CreatedUserDto;
import io.cvvexxx.frontend.dto.user.LoginUserDto;
import io.cvvexxx.frontend.dto.user.NewUserDto;
import io.cvvexxx.frontend.exception.BadRequestException;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

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
            MultipartFile userAvatar,
            @RequestParam(name = "target", required = false) String target,
            Model model
    ) {
        try {
            CreatedUserDto createdUserDto = userRestClient.registerUser(newUserDto, userAvatar);
            log.info("Successfully registered user: {}", createdUserDto.username());
            model.addAttribute("target", target);
            return "redirect:/login";
        } catch (BadRequestException exception) {
            model.addAttribute("payload", newUserDto);
            model.addAttribute("errors", exception.getErrors());
            return "security/registration";
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
            log.info("access token {}", tokenResponse.accessToken());
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
        UUID userId = extractUserId(accessToken);
        KeycloakJwtAuthenticationToken authToken = new KeycloakJwtAuthenticationToken(
                username,
                userId,
                accessToken,
                refreshToken,
                authorities
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
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

    private UUID extractUserId(String accessToken) {
        try {
            String[] chunks = accessToken.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(chunks[1]));
            JsonNode rootNode = objectMapper.readTree(payloadJson);
            return UUID.fromString(rootNode.path("sub").asText());
        } catch (Exception e) {
            log.error("Failed to parse 'sub' from access token", e);
            return null;
        }
    }
}