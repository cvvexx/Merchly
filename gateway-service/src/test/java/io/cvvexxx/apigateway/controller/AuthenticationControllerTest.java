package io.cvvexxx.apigateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cvvexxx.apigateway.client.KeycloakRestClient;
import io.cvvexxx.apigateway.dto.KeycloakTokenResponse;
import io.cvvexxx.apigateway.dto.LoginUserDto;
import io.cvvexxx.apigateway.security.JwtUtils;
import io.cvvexxx.apigateway.security.KeycloakJwtAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ConcurrentModel;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private final JwtUtils jwtUtils = new JwtUtils(new ObjectMapper());

    @Mock
    private KeycloakRestClient keycloakClient;
    private AuthenticationController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthenticationController(keycloakClient, jwtUtils);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private String dummyJwt(UUID userId, List<String> roles) {
        String rolesJson = roles.stream().map(r -> "\"" + r + "\"").reduce((a, b) -> a + "," + b).orElse("");
        String payloadJson = "{\"sub\":\"" + userId + "\",\"realm_access\":{\"roles\":[" + rolesJson + "]}}";
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        return header + "." + payload + ".signature";
    }

    @Test
    @DisplayName("getLoginPage: наполняет модель target-параметром и возвращает страницу логина шлюза")
    void getLoginPage_ShouldPopulateModelWithTargetAndReturnLoginPage() {
        var model = new ConcurrentModel();

        String result = controller.getLoginPage(model, "/orders");

        assertEquals("login", result);
        assertEquals("/orders", model.getAttribute("target"));
    }

    @Test
    @DisplayName("при успешном логине кладёт токены в сессию шлюза и делает редирект на target")
    void loginUser_WhenValidAndTargetProvided_ShouldAuthenticateAndRedirectToTarget() {
        UUID userId = UUID.randomUUID();
        String accessToken = dummyJwt(userId, List.of("user", "ROLE_admin"));
        when(keycloakClient.login("johndoe", "password"))
                .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));
        MockHttpServletRequest request = new MockHttpServletRequest();

        String result = controller.loginUser(new LoginUserDto("johndoe", "password"), "/orders", request);

        assertEquals("redirect:/orders", result);
        var authentication = (KeycloakJwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("johndoe", authentication.getPrincipal());
        assertEquals(userId, authentication.getUserId());
        assertEquals(accessToken, authentication.getAccessToken());
        assertEquals("refresh-token", authentication.getRefreshToken());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_user")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin")));
        assertNotNull(request.getSession(false));
    }

    @Test
    @DisplayName("логин выбрасывает до-логиновую сессию: защита от фиксации")
    void loginUser_ShouldReplacePreLoginSession() {
        String accessToken = dummyJwt(UUID.randomUUID(), List.of());
        when(keycloakClient.login("johndoe", "password"))
                .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));
        MockHttpServletRequest request = new MockHttpServletRequest();
        String preLoginSessionId = request.getSession(true).getId();

        controller.loginUser(new LoginUserDto("johndoe", "password"), null, request);

        assertNotNull(request.getSession(false));
        assertNotEquals(preLoginSessionId, request.getSession(false).getId());
    }

    @Test
    @DisplayName("при успешном логине без target делает редирект на список товаров")
    void loginUser_WhenValidAndNoTarget_ShouldRedirectToProductsList() {
        String accessToken = dummyJwt(UUID.randomUUID(), List.of());
        when(keycloakClient.login("johndoe", "password"))
                .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));
        MockHttpServletRequest request = new MockHttpServletRequest();

        String result = controller.loginUser(new LoginUserDto("johndoe", "password"), null, request);

        assertEquals("redirect:/catalogue/products/list", result);
    }

    @Test
    @DisplayName("при ошибке логина делает редирект на /login?error=true с закодированным target")
    void loginUser_WhenKeycloakLoginFails_ShouldRedirectToLoginWithErrorAndEncodedTarget() {
        when(keycloakClient.login("johndoe", "wrong-password"))
                .thenThrow(new RuntimeException("invalid_grant"));
        MockHttpServletRequest request = new MockHttpServletRequest();

        String result = controller.loginUser(new LoginUserDto("johndoe", "wrong-password"), "/orders?x=y", request);

        assertEquals("redirect:/login?error=true&target=%2Forders%3Fx%3Dy", result);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
