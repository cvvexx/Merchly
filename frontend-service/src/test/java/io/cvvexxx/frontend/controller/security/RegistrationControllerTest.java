package io.cvvexxx.frontend.controller.security;

import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.user.CreatedUserDto;
import io.cvvexxx.frontend.dto.user.NewUserDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.exception.FieldAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private RestClientUserPublicRestClient userRestClient;
    private RegistrationController controller;

    @BeforeEach
    void setUp() {
        controller = new RegistrationController(userRestClient);
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
}
