package io.cvvexxx.users.controller;

import io.cvvexxx.users.domain.Gender;
import io.cvvexxx.users.dto.NewUserDto;
import io.cvvexxx.users.dto.UpdateUserDto;
import io.cvvexxx.users.dto.UserCreatedDto;
import io.cvvexxx.users.dto.UserInfoDto;
import io.cvvexxx.users.dto.UserProfilePublicDto;
import io.cvvexxx.users.service.user.DefaultUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private DefaultUserService userService;

    @InjectMocks
    private UsersController controller;

    @Test
    @DisplayName("registerUser: при ошибках валидации выбрасывает BindException и не регистрирует пользователя")
    void registerUser_WhenBindingResultHasErrors_ShouldThrowBindException() {
        // given
        NewUserDto dto = newUserDto();
        MultipartFile avatar = new MockMultipartFile("image", new byte[0]);
        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "newUserDto");
        bindingResult.reject("username", "must not be blank");

        // when / then
        assertThrows(BindException.class, () -> controller.registerUser(dto, avatar, bindingResult));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("registerUser: при валидном запросе делегирует регистрацию сервису")
    void registerUser_WhenValid_ShouldDelegateToService() throws BindException {
        // given
        NewUserDto dto = newUserDto();
        MultipartFile avatar = new MockMultipartFile("image", new byte[0]);
        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "newUserDto");
        UserCreatedDto created = new UserCreatedDto(UUID.randomUUID(), dto.username());
        when(userService.registerUserInKeycloakAndLocalDb(dto, avatar)).thenReturn(created);

        // when
        ResponseEntity<UserCreatedDto> response = controller.registerUser(dto, avatar, bindingResult);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(created, response.getBody());
        verify(userService).registerUserInKeycloakAndLocalDb(dto, avatar);
    }

    @Test
    @DisplayName("getSecurityUserInfo: извлекает userId из sub-claim и возвращает информацию о пользователе")
    void getSecurityUserInfo_ShouldReturnInfoForCurrentUser() {
        // given
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        UserInfoDto info = new UserInfoDto("username", "email@test.com", "M", LocalDate.of(2000, 1, 1), Set.of("USER"), null);
        when(userService.getUserInfo(userId)).thenReturn(info);

        // when
        ResponseEntity<UserInfoDto> response = controller.getSecurityUserInfo(jwt);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(info, response.getBody());
        verify(userService).getUserInfo(userId);
    }

    @Test
    @DisplayName("updateUserInfo: при ошибках валидации выбрасывает BindException и не обновляет пользователя")
    void updateUserInfo_WhenBindingResultHasErrors_ShouldThrowBindException() {
        // given
        UpdateUserDto dto = updateUserDto();
        Jwt jwt = jwtFor(UUID.randomUUID());
        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "updateUserDto");
        bindingResult.reject("email", "must be valid");

        // when / then
        assertThrows(BindException.class, () -> controller.updateUserInfo(dto, null, jwt, bindingResult));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("updateUserInfo: при валидном запросе обновляет данные текущего пользователя")
    void updateUserInfo_WhenValid_ShouldDelegateToServiceForCurrentUser() throws BindException {
        // given
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        UpdateUserDto dto = updateUserDto();
        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "updateUserDto");

        // when
        ResponseEntity<Void> response = controller.updateUserInfo(dto, null, jwt, bindingResult);

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).updateUserInfo(userId, dto, null);
    }

    @Test
    @DisplayName("getPublicUserProfile: делегирует поиск публичного профиля по username")
    void getPublicUserProfile_ShouldDelegateToService() {
        // given
        String username = "public_user";
        UserProfilePublicDto profile = new UserProfilePublicDto(UUID.randomUUID(), username, "M", LocalDate.of(1995, 5, 5), null);
        when(userService.getPublicUserProfile(username)).thenReturn(profile);

        // when
        ResponseEntity<UserProfilePublicDto> response = controller.getPublicUserProfile(username);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(profile, response.getBody());
        verify(userService).getPublicUserProfile(username);
    }

    private Jwt jwtFor(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .build();
    }

    private NewUserDto newUserDto() {
        return new NewUserDto("John", "Doe", "testuser", "password", "email@test.com", Gender.M, LocalDate.of(2000, 1, 1), false);
    }

    private UpdateUserDto updateUserDto() {
        return new UpdateUserDto("new_username", "new@test.com", Gender.M, LocalDate.of(1999, 1, 1));
    }
}
