package io.cvvexxx.users.service;

import io.cvvexxx.users.domain.Gender;
import io.cvvexxx.users.dto.*;
import io.cvvexxx.users.entity.Role;
import io.cvvexxx.users.entity.User;
import io.cvvexxx.users.repository.RoleRepository;
import io.cvvexxx.users.repository.UserRepository;
import io.cvvexxx.users.service.minio.DefaultMinioService;
import io.cvvexxx.users.service.user.DefaultUserService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultUserServiceTest {

    private final String REALM = "test-realm";
    private final UUID USER_ID = UUID.randomUUID();
    private final String KEYCLOAK_ID = UUID.randomUUID().toString();
    @Mock
    private Keycloak keycloak;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private DefaultMinioService defaultMinioService;
    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;
    @Mock
    private Response keycloakResponse;
    @Mock
    private MultipartFile avatarFile;
    @InjectMocks
    private DefaultUserService defaultUserService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(defaultUserService, "realm", REALM);
    }

    @Nested
    @DisplayName("Тесты метода registerUserInKeycloakAndLocalDb")
    class RegisterUserTests {

        private NewUserDto newUserDto;

        @BeforeEach
        void setUp() {
            newUserDto = new NewUserDto("testuser", "password", "email@test.com", "John", "Doe", null, LocalDate.of(2000, 1, 1));
            lenient().when(keycloak.realm(REALM)).thenReturn(realmResource);
            lenient().when(realmResource.users()).thenReturn(usersResource);
        }

        @Test
        @DisplayName("Успешная регистрация пользователя с аватаром")
        void registerUser_Success() {
            // given
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(keycloakResponse);
            when(keycloakResponse.getStatus()).thenReturn(201);
            when(keycloakResponse.getHeaderString("Location")).thenReturn("http://localhost/auth/admin/realms/" + REALM + "/users/" + KEYCLOAK_ID);

            when(avatarFile.isEmpty()).thenReturn(false);
            when(defaultMinioService.upload(avatarFile)).thenReturn("avatar.png");

            Role role = new Role();
            role.setRole("USER");
            when(roleRepository.findByRole("USER")).thenReturn(role);

            User savedUser = new User(UUID.fromString(KEYCLOAK_ID), "testuser", "email@test.com", null, LocalDate.of(2000, 1, 1), Set.of(role), "avatar.png");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // when
            UserCreatedDto result = defaultUserService.registerUserInKeycloakAndLocalDb(newUserDto, avatarFile);

            // then
            assertNotNull(result);
            assertEquals(UUID.fromString(KEYCLOAK_ID), result.id());
            assertEquals("testuser", result.username());

            verify(defaultMinioService, times(1)).upload(avatarFile);
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Ошибка 409 (Конфликт) от Keycloak выбрасывает IllegalArgumentException")
        void registerUser_KeycloakConflict_ShouldThrowException() {
            // given
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(keycloakResponse);
            when(keycloakResponse.getStatus()).thenReturn(409);

            // when & then
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> defaultUserService.registerUserInKeycloakAndLocalDb(newUserDto, null)
            );

            assertTrue(ex.getMessage().contains("already exists"));
            verifyNoInteractions(userRepository, defaultMinioService);
        }

        @Test
        @DisplayName("Ошибка сохранения в БД вызывает откат (удаление из Keycloak и MinIO)")
        void registerUser_DbFailure_ShouldRollback() {
            // given
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(keycloakResponse);
            when(keycloakResponse.getStatus()).thenReturn(201);
            when(keycloakResponse.getHeaderString("Location")).thenReturn("http://localhost:8090/" + KEYCLOAK_ID);

            when(avatarFile.isEmpty()).thenReturn(false);
            when(defaultMinioService.upload(avatarFile)).thenReturn("avatar.png");

            when(roleRepository.findByRole("USER")).thenReturn(null);

            when(usersResource.get(KEYCLOAK_ID)).thenReturn(userResource);

            // when & then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> defaultUserService.registerUserInKeycloakAndLocalDb(newUserDto, avatarFile)
            );

            verify(userResource, times(1)).remove();
            verify(defaultMinioService, times(1)).removeObject("avatar.png");
        }
    }

    @Nested
    @DisplayName("Тесты метода getUserInfo")
    class GetUserInfoTests {

        @Test
        @DisplayName("Успешное получение информации с фильтрацией системных ролей")
        void getUserInfo_Success() {
            // given
            Role role1 = new Role();
            role1.setRole("USER");
            Role role2 = new Role();
            role2.setRole("offline_access");

            User user = new User();
            user.setUsername("testuser");
            user.setEmail("test@test.com");
            user.setGender(Gender.M);
            user.setRoles(Set.of(role1, role2));

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // when
            UserInfoDto result = defaultUserService.getUserInfo(USER_ID);

            // then
            assertEquals("testuser", result.username());
            assertFalse(result.roles().contains("ROLE_USER"));
        }
    }

    @Nested
    @DisplayName("Тесты метода updateUserInfo")
    class UpdateUserInfoTests {

        private UpdateUserDto updateDto;

        @BeforeEach
        void setUp() {
            updateDto = new UpdateUserDto("new_username", "new@test.com", null, LocalDate.now());
            lenient().when(keycloak.realm(REALM)).thenReturn(realmResource);
            lenient().when(realmResource.users()).thenReturn(usersResource);
        }

        @Test
        @DisplayName("Успешное обновление пользователя с заменой аватара")
        void updateUserInfo_WithNewAvatar_ShouldUpdateDbAndMinioAndKeycloak() {
            // given
            User user = new User();
            user.setId(USER_ID);
            user.setUsername("old_username");
            user.setAvatarFileName("old_avatar.png");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(usersResource.get(USER_ID.toString())).thenReturn(userResource);
            when(userResource.toRepresentation()).thenReturn(new UserRepresentation());

            when(avatarFile.isEmpty()).thenReturn(false);
            when(defaultMinioService.upload(avatarFile)).thenReturn("new_avatar.png");

            // when
            defaultUserService.updateUserInfo(USER_ID, updateDto, avatarFile);

            // then
            ArgumentCaptor<UserRepresentation> repCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
            verify(userResource, times(1)).update(repCaptor.capture());
            assertEquals("new_username", repCaptor.getValue().getUsername());

            verify(defaultMinioService, times(1)).upload(avatarFile);
            verify(defaultMinioService, times(1)).removeObject("old_avatar.png");

            assertEquals("new_avatar.png", user.getAvatarFileName());
            assertEquals("new_username", user.getUsername());
        }

        @Test
        @DisplayName("Если Keycloak падает, выбрасывается RuntimeException")
        void updateUserInfo_KeycloakFails_ShouldThrowException() {
            // given
            User user = new User();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(usersResource.get(USER_ID.toString())).thenThrow(new RuntimeException("Keycloak timeout"));

            // when & then
            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> defaultUserService.updateUserInfo(USER_ID, updateDto, null)
            );

            assertTrue(ex.getMessage().contains("Failed to update user in Keycloak"));
        }
    }

    @Nested
    @DisplayName("Тесты методов поиска списков (findUsersByIds и findUserById)")
    class FindUsersTests {

        @Test
        @DisplayName("findUsersByIds: игнорирует null ID и возвращает корректный список")
        void findUsersByIds_Success() {
            // given
            User user = new User();
            user.setId(USER_ID);
            user.setUsername("owner");

            when(userRepository.findAllByIdIn(List.of(USER_ID))).thenReturn(List.of(user));

            // when
            List<UserProductOwnerDto> result = defaultUserService.findUsersByIds(Arrays.asList(USER_ID, null));

            // then
            assertEquals(1, result.size());
            assertEquals("owner", result.get(0).username());
        }

        @Test
        @DisplayName("findUsersByIds: если передан пустой список валидных ID, возвращает пустой список")
        void findUsersByIds_EmptyValidIds_ReturnsEmpty() {
            List<UserProductOwnerDto> result = defaultUserService.findUsersByIds(Collections.singletonList(null));
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("findUserById: если ID null, возвращает анонимного пользователя")
        void findUserById_NullId_ReturnsAnonymous() {
            UserProductOwnerDto result = defaultUserService.findUserById(null);
            assertEquals("Неизвестен", result.username());
            assertNull(result.id());
        }
    }

    @Nested
    @DisplayName("Тесты метода getPublicUserProfile")
    class GetPublicUserProfileTests {

        @Test
        @DisplayName("Успешное получение публичного профиля по юзернейму")
        void getPublicUserProfile_Success() {
            // given
            User user = new User();
            user.setId(USER_ID);
            user.setUsername("public_user");
            user.setGender(Gender.M);

            when(userRepository.findByUsername("public_user")).thenReturn(Optional.of(user));

            // when
            UserProfilePublicDto result = defaultUserService.getPublicUserProfile("public_user");

            // then
            assertEquals(USER_ID, result.id());
            assertEquals("public_user", result.username());
        }

        @Test
        @DisplayName("Если пользователь не найден, выбрасывается UsernameNotFoundException")
        void getPublicUserProfile_NotFound_ShouldThrowException() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThrows(
                    UsernameNotFoundException.class,
                    () -> defaultUserService.getPublicUserProfile("unknown")
            );
        }
    }
}