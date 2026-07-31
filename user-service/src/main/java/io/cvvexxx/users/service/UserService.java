package io.cvvexxx.users.service;

import io.cvvexxx.users.domain.Gender;
import io.cvvexxx.users.dto.*;
import io.cvvexxx.users.dto.cart.AddToCartDto;
import io.cvvexxx.users.entity.Role;
import io.cvvexxx.users.entity.User;
import io.cvvexxx.users.repository.RoleRepository;
import io.cvvexxx.users.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MinioService minioService;

    @Value("${keycloak.realm}")
    private String realm;

    @Transactional
    public UserCreatedDto registerUserInKeycloakAndLocalDb(NewUserDto newUserDto, MultipartFile userAvatar) {
        log.info("Received request to register user in keycloak and local db {}", newUserDto);

        UserRepresentation user = getUserRepresentation(newUserDto);
        UsersResource usersResource = keycloak.realm(realm).users();

        String keycloakUserId;

        // 1. Создаем пользователя в Keycloak
        try (Response response = usersResource.create(user)) {
            log.info("Keycloak response status: {}", response.getStatus());

            if (response.getStatus() == 409) {
                throw new IllegalArgumentException(
                        "User with username '%s' or email '%s' already exists"
                                .formatted(newUserDto.username(), newUserDto.email())
                );
            }

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
            }

            String locationHeader = response.getHeaderString("Location");
            if (locationHeader == null || locationHeader.isBlank()) {
                throw new IllegalStateException("Keycloak response did not include a Location header");
            }

            keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
            log.info("Keycloak User ID {}", keycloakUserId);
        }

        // 2. Загружаем аватар только ПОСЛЕ успешного создания в Keycloak
        String userAvatarFileName = null;
        if (userAvatar != null && !userAvatar.isEmpty()) {
            userAvatarFileName = minioService.upload(userAvatar);
        }

        // 3. Сохраняем в локальную БД с откатом Keycloak при ошибке
        try {
            Role defaultRole = roleRepository.findByRole("USER");
            if (defaultRole == null) {
                throw new IllegalStateException("Default role 'USER' not found in database");
            }

            User localUser = new User(
                    UUID.fromString(keycloakUserId),
                    newUserDto.username(),
                    newUserDto.email(),
                    newUserDto.gender(),
                    newUserDto.birthDate(),
                    Set.of(defaultRole),
                    userAvatarFileName
            );

            User savedUser = userRepository.save(localUser);
            log.info("Saved user {}", savedUser);

            return new UserCreatedDto(savedUser.getId(), savedUser.getUsername());

        } catch (Exception e) {
            log.error("Failed to save user in local DB. Rolling back Keycloak user {}", keycloakUserId, e);

            // Компенсирующее действие: удаляем юзера из Keycloak, если локальная БД упала
            try {
                usersResource.get(keycloakUserId).remove();
            } catch (Exception ex) {
                log.error("Failed to rollback Keycloak user deletion for ID: {}", keycloakUserId, ex);
            }

            // Если файл успел загрузиться в MinIO — чистим
            if (userAvatarFileName != null) {
                try {
                    minioService.removeObject(userAvatarFileName);
                } catch (Exception ex) {
                    log.error("Failed to cleanup MinIO file {}", userAvatarFileName, ex);
                }
            }

            throw e;
        }
    }

    private UserRepresentation getUserRepresentation(NewUserDto newUserDto) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newUserDto.password());
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setFirstName(newUserDto.firstName());
        user.setLastName(newUserDto.lastName());
        user.setUsername(newUserDto.username());
        user.setEmail(newUserDto.email());
        user.setEnabled(true);
        user.setCredentials(Collections.singletonList(credential));
        return user;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user_info", key = "#userId")
    public UserInfoDto getUserInfo(UUID userId) {
        User user = findUser(userId);

        Set<String> cleanRoles = mapRoleToString(user.getRoles())
                .stream()
                .filter(role -> !role.startsWith("default-roles")
                        && !role.equals("offline_access")
                        && !role.equals("uma_authorization"))
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.toSet());


        return new UserInfoDto(
                user.getUsername(),
                user.getEmail(),
                user.getGender().name(),
                user.getBirthDate(),
                cleanRoles,
                user.getAvatarFileName()
        );
    }

    @Transactional(readOnly = true)
    public List<UserProductOwnerDto> findUsersByIds(List<UUID> ids) {

        List<UUID> validIds = ids.stream()
                .filter(Objects::nonNull)
                .toList();

        if (validIds.isEmpty()) {
            return List.of();
        }

        return userRepository.findAllByIdIn(validIds).stream()
                .map(user -> new UserProductOwnerDto(
                        user.getId(),
                        user.getUsername(),
                        user.getAvatarFileName()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users_owner", key = "#userId")
    public UserProductOwnerDto findUserById(UUID userId) {

        if (userId == null) {
            return new UserProductOwnerDto(null, "Неизвестен", "/images/default-user-avatar.png");
        }
        User user = findUser(userId);

        return new UserProductOwnerDto(
                user.getId(),
                user.getUsername(),
                user.getAvatarFileName()
        );
    }

    @Transactional
    public void updateUserInfo(
            UUID userId,
            UpdateUserDto updateUserDto,
            MultipartFile userAvatar
    ) {
        User user = findUser(userId);

        updateKeycloakUser(user.getId().toString(), updateUserDto);

        String oldImageFileName = user.getAvatarFileName();

        if (userAvatar != null && !userAvatar.isEmpty()) {
            String newImageFileName = minioService.upload(userAvatar);
            user.setAvatarFileName(newImageFileName);

            if (oldImageFileName != null && !oldImageFileName.isBlank()) {
                try {
                    minioService.removeObject(oldImageFileName);
                } catch (Exception e) {
                    log.error("Failed to delete old avatar {} from MinIO for user {}",
                            oldImageFileName, user.getUsername(), e);
                }
            }
        }

        user.setUsername(updateUserDto.username());
        user.setEmail(updateUserDto.email());
        user.setGender(updateUserDto.gender());
        user.setBirthDate(updateUserDto.birthDate());
    }

    private void updateKeycloakUser(String keycloakUserId, UpdateUserDto updateUserDto) {
        try {
            var userResource = keycloak.realm(realm).users().get(keycloakUserId);

            UserRepresentation userRep = userResource.toRepresentation();

            userRep.setUsername(updateUserDto.username());
            userRep.setEmail(updateUserDto.email());

            userResource.update(userRep);
            log.info("Successfully updated Keycloak user with ID: {}", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to update user in Keycloak for ID {}: {}", keycloakUserId, e.getMessage(), e);
            throw new RuntimeException("Failed to update user in Keycloak", e);
        }
    }


    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user with login: %s"
                        .formatted(userId)));
    }

    private Set<String> mapRoleToString(Set<Role> roles) {
        return roles.stream()
                .map(Role::getRole)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());
    }
}