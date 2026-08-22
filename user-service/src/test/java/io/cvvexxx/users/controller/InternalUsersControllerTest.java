package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.UserProductOwnerDto;
import io.cvvexxx.users.service.user.DefaultUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalUsersControllerTest {

    @Mock
    private DefaultUserService userService;

    @InjectMocks
    private InternalUsersController controller;

    @Test
    @DisplayName("getUserByIds: если ids равен null, возвращает 400 и не обращается к сервису")
    void getUserByIds_WhenIdsIsNull_ShouldReturnBadRequestWithoutCallingService() {
        // given / when
        ResponseEntity<List<UserProductOwnerDto>> response = controller.getUserByIds(null);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("getUserByIds: если ids пуст, возвращает 400 и не обращается к сервису")
    void getUserByIds_WhenIdsIsEmpty_ShouldReturnBadRequestWithoutCallingService() {
        // given / when
        ResponseEntity<List<UserProductOwnerDto>> response = controller.getUserByIds(List.of());

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("getUserByIds: если ids переданы, делегирует поиск в сервис")
    void getUserByIds_WhenIdsProvided_ShouldDelegateToService() {
        // given
        UUID userId = UUID.randomUUID();
        List<UUID> ids = List.of(userId);
        List<UserProductOwnerDto> users = List.of(new UserProductOwnerDto(userId, "owner", null));
        when(userService.findUsersByIds(ids)).thenReturn(users);

        // when
        ResponseEntity<List<UserProductOwnerDto>> response = controller.getUserByIds(ids);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
        verify(userService).findUsersByIds(ids);
    }

    @Test
    @DisplayName("getUserById: делегирует поиск пользователя по id в сервис")
    void getUserById_ShouldDelegateToService() {
        // given
        UUID userId = UUID.randomUUID();
        UserProductOwnerDto user = new UserProductOwnerDto(userId, "owner", null);
        when(userService.findUserById(userId)).thenReturn(user);

        // when
        ResponseEntity<UserProductOwnerDto> response = controller.getUserById(userId);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
        verify(userService).findUserById(userId);
    }
}
