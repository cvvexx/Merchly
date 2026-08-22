package io.cvvexxx.frontend.controller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cvvexxx.frontend.client.keycloak.KeycloakRestClient;
import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.keycloak.KeycloakTokenResponse;
import io.cvvexxx.frontend.dto.user.CreatedUserDto;
import io.cvvexxx.frontend.dto.user.LoginUserDto;
import io.cvvexxx.frontend.dto.user.NewUserDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.exception.FieldAlreadyExistsException;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private RestClientUserPublicRestClient userRestClient;
    @Mock
    private KeycloakRestClient keycloakClient;
    private AuthenticationController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthenticationController(userRestClient, keycloakClient, objectMapper);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getLoginPage: наполняет модель target-параметром и возвращает страницу логина")
    void getLoginPage_ShouldPopulateModelWithTargetAndReturnLoginPage() {
        // given
        var model = new ConcurrentModel();

        // when
        String result = controller.getLoginPage(model, "/orders");

        // then
        assertEquals("security/login", result);
        assertEquals("/orders", model.getAttribute("target"));
    }

    @Test
    @DisplayName("getRegistrationPage: наполняет модель target-параметром и возвращает страницу регистрации")
    void getRegistrationPage_ShouldPopulateModelWithTargetAndReturnRegistrationPage() {
        // given
        var model = new ConcurrentModel();

        // when
        String result = controller.getRegistrationPage(model, "/orders");

        // then
        assertEquals("security/registration", result);
        assertEquals("/orders", model.getAttribute("target"));
    }

    private NewUserDto newUserDto() {
        return new NewUserDto("John", "Doe", "johndoe", "password", "john@mail.com", "MALE", LocalDate.of(1990, 1, 1));
    }

    private String dummyJwt(UUID userId, List<String> roles) {
        String rolesJson = roles.stream().map(r -> "\"" + r + "\"").reduce((a, b) -> a + "," + b).orElse("");
        String payloadJson = "{\"sub\":\"" + userId + "\",\"realm_access\":{\"roles\":[" + rolesJson + "]}}";
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        return header + "." + payload + ".signature";
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUserTests {

        @Test
        @DisplayName("при успешной регистрации делает редирект на /login")
        void registerUser_WhenValid_ShouldRedirectToLogin() {
            // given
            NewUserDto newUserDto = newUserDto();
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();
            when(userRestClient.registerUser(newUserDto, avatar))
                    .thenReturn(new CreatedUserDto(UUID.randomUUID(), "username"));

            // when
            String result = controller.registerUser(newUserDto, avatar, "/orders", model);

            // then
            assertEquals("redirect:/login", result);
            assertEquals("/orders", model.getAttribute("target"));
        }

        @Test
        @DisplayName("при ошибке валидации возвращает страницу регистрации с ошибками")
        void registerUser_WhenBadRequest_ShouldReturnRegistrationPageWithErrors() {
            // given
            NewUserDto newUserDto = newUserDto();
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();
            doThrow(new BadRequestException(List.of("error1")))
                    .when(userRestClient).registerUser(newUserDto, avatar);

            // when
            String result = controller.registerUser(newUserDto, avatar, null, model);

            // then
            assertEquals("security/registration", result);
            assertEquals(newUserDto, model.getAttribute("payload"));
            assertEquals(List.of("error1"), model.getAttribute("errors"));
        }

        @Test
        @DisplayName("если поле уже занято, возвращает страницу регистрации с сообщением об ошибке")
        void registerUser_WhenFieldAlreadyExists_ShouldReturnRegistrationPageWithErrorMessage() {
            // given
            NewUserDto newUserDto = newUserDto();
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();
            doThrow(new FieldAlreadyExistsException("username", "Username already taken"))
                    .when(userRestClient).registerUser(newUserDto, avatar);

            // when
            String result = controller.registerUser(newUserDto, avatar, null, model);

            // then
            assertEquals("security/registration", result);
            assertEquals(newUserDto, model.getAttribute("payload"));
            assertEquals(List.of("Username already taken"), model.getAttribute("errors"));
        }

        @Test
        @DisplayName("при непредвиденной ошибке делает редирект на страницу регистрации с error=invalid_data")
        void registerUser_WhenUnexpectedError_ShouldRedirectWithErrorParam() {
            // given
            NewUserDto newUserDto = newUserDto();
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();
            doThrow(new RuntimeException("boom")).when(userRestClient).registerUser(newUserDto, avatar);

            // when
            String result = controller.registerUser(newUserDto, avatar, null, model);

            // then
            assertEquals("redirect:/registration?error=invalid_data", result);
        }
    }

    @Nested
    @DisplayName("loginUser")
    class LoginUserTests {

        @Test
        @DisplayName("при успешном логине аутентифицирует пользователя в сессии и делает редирект на target")
        void loginUser_WhenValidAndTargetProvided_ShouldAuthenticateAndRedirectToTarget() {
            // given
            LoginUserDto loginUserDto = new LoginUserDto("johndoe", "password");
            UUID userId = UUID.randomUUID();
            String accessToken = dummyJwt(userId, List.of("user", "ROLE_admin"));
            when(keycloakClient.login("johndoe", "password"))
                    .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            String result = controller.loginUser(loginUserDto, "/orders", request);

            // then
            assertEquals("redirect:/orders", result);
            var authentication = (KeycloakJwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication);
            assertEquals("johndoe", authentication.getPrincipal());
            assertEquals(userId, authentication.getUserId());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_user")));
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_admin")));
            assertNotNull(request.getSession(false));
        }

        @Test
        @DisplayName("при успешном логине без target делает редирект на список товаров")
        void loginUser_WhenValidAndNoTarget_ShouldRedirectToProductsList() {
            // given
            LoginUserDto loginUserDto = new LoginUserDto("johndoe", "password");
            String accessToken = dummyJwt(UUID.randomUUID(), List.of());
            when(keycloakClient.login("johndoe", "password"))
                    .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            String result = controller.loginUser(loginUserDto, null, request);

            // then
            assertEquals("redirect:/catalogue/products/list", result);
        }

        @Test
        @DisplayName("при ошибке логина делает редирект на /login?error=true с закодированным target")
        void loginUser_WhenKeycloakLoginFails_ShouldRedirectToLoginWithErrorAndEncodedTarget() {
            // given
            LoginUserDto loginUserDto = new LoginUserDto("johndoe", "wrong-password");
            when(keycloakClient.login("johndoe", "wrong-password"))
                    .thenThrow(new RuntimeException("invalid_grant"));
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            String result = controller.loginUser(loginUserDto, "/orders?x=y", request);

            // then
            assertEquals("redirect:/login?error=true&target=%2Forders%3Fx%3Dy", result);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }
}
