package io.cvvexxx.frontend.controller.user;

import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.user.UpdateUserDto;
import io.cvvexxx.frontend.dto.user.UserInfoDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.exception.FieldAlreadyExistsException;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private RestClientUserPublicRestClient restClient;

    @Mock
    private ImageUrlFormatter imageUrlFormatter;

    @InjectMocks
    private UserController controller;

    @Test
    @DisplayName("user (@ModelAttribute): делегирует получение данных пользователя в rest-клиент")
    void user_ShouldDelegateToRestClient() {
        // given
        UserInfoDto userInfo = userInfo();
        when(restClient.getUserInfo()).thenReturn(userInfo);

        // when
        UserInfoDto result = controller.user();

        // then
        assertEquals(userInfo, result);
    }

    @Test
    @DisplayName("userProfilePage: наполняет модель данными пользователя и URL аватара")
    void userProfilePage_ShouldPopulateModelWithUserAndAvatar() {
        // given
        UserInfoDto userInfo = userInfo();
        when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");
        var model = new ConcurrentModel();

        // when
        String result = controller.userProfilePage(userInfo, model);

        // then
        assertEquals("user/profile", result);
        assertEquals(userInfo, model.getAttribute("user"));
        assertEquals("/img/avatar.png", model.getAttribute("userAvatar"));
    }

    @Test
    @DisplayName("editProfilePage: наполняет модель данными пользователя и URL аватара")
    void editProfilePage_ShouldPopulateModelWithUserAndAvatar() {
        // given
        UserInfoDto userInfo = userInfo();
        when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");
        var model = new ConcurrentModel();

        // when
        String result = controller.editProfilePage(userInfo, model);

        // then
        assertEquals("user/edit", result);
        assertEquals(userInfo, model.getAttribute("user"));
        assertEquals("/img/avatar.png", model.getAttribute("userAvatar"));
    }

    @Nested
    @DisplayName("editUserProfile")
    class EditUserProfileTests {

        @Test
        @DisplayName("при успешном обновлении делает редирект на /profile")
        void editUserProfile_WhenValid_ShouldRedirectToProfile() {
            // given
            UserInfoDto userInfo = userInfo();
            UpdateUserDto updateUserDto = new UpdateUserDto("newname", "new@mail.com", "MALE", LocalDate.of(1995, 5, 5));
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();

            // when
            String result = controller.editUserProfile(userInfo, avatar, updateUserDto, model);

            // then
            assertEquals("redirect:/profile", result);
            verify(restClient).updateUserInfo(updateUserDto, avatar);
        }

        @Test
        @DisplayName("при ошибке валидации возвращает страницу редактирования с ошибками")
        void editUserProfile_WhenBadRequest_ShouldReturnEditPageWithErrors() {
            // given
            UserInfoDto userInfo = userInfo();
            UpdateUserDto updateUserDto = new UpdateUserDto("", "invalid-email", null, null);
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();

            doThrow(new BadRequestException(List.of("error1")))
                    .when(restClient).updateUserInfo(updateUserDto, avatar);
            when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");

            // when
            String result = controller.editUserProfile(userInfo, avatar, updateUserDto, model);

            // then
            assertEquals("user/edit", result);
            assertEquals("/img/avatar.png", model.getAttribute("userAvatar"));
            assertEquals(updateUserDto, model.getAttribute("payload"));
            assertEquals(List.of("error1"), model.getAttribute("errors"));
        }

        @Test
        @DisplayName("если username/email уже занят, возвращает страницу регистрации с сообщением об ошибке")
        void editUserProfile_WhenFieldAlreadyExists_ShouldReturnRegistrationPageWithErrorMessage() {
            // given
            UserInfoDto userInfo = userInfo();
            UpdateUserDto updateUserDto = new UpdateUserDto("taken", "taken@mail.com", null, null);
            var avatar = new MockMultipartFile("userAvatar", "avatar.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();

            doThrow(new FieldAlreadyExistsException("username", "Username already exists"))
                    .when(restClient).updateUserInfo(updateUserDto, avatar);
            when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");

            // when
            String result = controller.editUserProfile(userInfo, avatar, updateUserDto, model);

            // then
            assertEquals("security/registration", result);
            assertEquals("/img/avatar.png", model.getAttribute("userAvatar"));
            assertEquals(updateUserDto, model.getAttribute("payload"));
            assertEquals(List.of("Username already exists"), model.getAttribute("errors"));
        }
    }

    private UserInfoDto userInfo() {
        return new UserInfoDto("username", "user@mail.com", "MALE", LocalDate.of(1990, 1, 1), Set.of("USER"), "avatar.png");
    }
}
