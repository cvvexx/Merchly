package io.cvvexxx.frontend.controller.user;

import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.dto.user.UserProfilePublicDto;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicProfileControllerTest {

    @Mock
    private UserPublicRestClient userPublicRestClient;

    @Mock
    private ImageUrlFormatter imageUrlFormatter;

    @InjectMocks
    private PublicProfileController controller;

    @Test
    @DisplayName("если открыт собственный профиль, делает редирект на /profile и не обращается к user-service")
    void showPublicProfile_WhenOwnProfile_ShouldRedirectToProfileWithoutCallingUserService() {
        // given
        var model = new ConcurrentModel();

        // when
        String result = controller.showPublicProfile("johndoe", model, "JohnDoe");

        // then
        assertEquals("redirect:/profile", result);
        verifyNoInteractions(userPublicRestClient, imageUrlFormatter);
    }

    @Test
    @DisplayName("если открыт чужой профиль, наполняет модель данными профиля и URL аватара")
    void showPublicProfile_WhenOtherUsersProfile_ShouldPopulateModelWithProfileAndAvatar() {
        // given
        UserProfilePublicDto profile = new UserProfilePublicDto(
                UUID.randomUUID(), "janedoe", "FEMALE", LocalDate.of(1990, 1, 1), "avatar.png"
        );
        when(userPublicRestClient.getUserProfile("janedoe")).thenReturn(profile);
        when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");
        var model = new ConcurrentModel();

        // when
        String result = controller.showPublicProfile("janedoe", model, "johndoe");

        // then
        assertEquals("user/public-profile", result);
        assertEquals(profile, model.getAttribute("profile"));
        assertEquals("/img/avatar.png", model.getAttribute("userAvatarUrl"));
    }
}
