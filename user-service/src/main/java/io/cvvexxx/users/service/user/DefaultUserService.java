package io.cvvexxx.users.service.user;

import io.cvvexxx.users.dto.*;
import io.cvvexxx.users.entity.Role;
import io.cvvexxx.users.entity.User;
import io.cvvexxx.users.exception.FieldAlreadyExistsException;
import io.cvvexxx.users.repository.RoleRepository;
import io.cvvexxx.users.repository.UserRepository;
import io.cvvexxx.users.service.minio.DefaultMinioService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultUserService implements UserService {
    private static final String KEYCLOAK_ADMIN_ROLE = "ROLE_ADMIN";
    private static final String LOCAL_ADMIN_ROLE = "ADMIN";
    private static final String LOCAL_USER_ROLE = "USER";

    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DefaultMinioService minioService;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    @Transactional
    public UserCreatedDto registerUserInKeycloakAndLocalDb(NewUserDto newUserDto, MultipartFile userAvatar) {

        UserRepresentation user = getUserRepresentation(newUserDto);
        UsersResource usersResource = keycloak.realm(realm).users();

        String keycloakUserId;

        try (Response response = usersResource.create(user)) {

            if (response.getStatus() == 409) {
                boolean usernameExists = !usersResource.searchByUsername(newUserDto.username(), true).isEmpty();
                boolean emailExists = !usersResource.searchByEmail(newUserDto.email(), true).isEmpty();

                if (usernameExists) {
                    throw new FieldAlreadyExistsException("username", newUserDto.username(),
                            "Пользователь с логином '%s' уже существует".formatted(newUserDto.username()));
                }

                if (emailExists) {
                    throw new FieldAlreadyExistsException("email", newUserDto.email(),
                            "Пользователь с email '%s' уже существует".formatted(newUserDto.email()));
                }

                throw new FieldAlreadyExistsException("usernameOrEmail",
                        "Пользователь с таким логином или email уже существует");
            }

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
            }

            String locationHeader = response.getHeaderString("Location");
            if (locationHeader == null || locationHeader.isBlank()) {
                throw new IllegalStateException("Keycloak response did not include a Location header");
            }

            keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
        }

        String userAvatarFileName = null;
        if (userAvatar != null && !userAvatar.isEmpty()) {
            userAvatarFileName = minioService.upload(userAvatar);
        }

        try {
            Set<Role> userRoles = setUserRoles(usersResource, newUserDto.isAdmin(), keycloakUserId);

            User localUser = new User(
                    UUID.fromString(keycloakUserId),
                    newUserDto.username(),
                    newUserDto.email(),
                    newUserDto.gender(),
                    newUserDto.birthDate(),
                    userRoles,
                    userAvatarFileName
            );

            User savedUser = userRepository.saveAndFlush(localUser);

            return new UserCreatedDto(savedUser.getId(), savedUser.getUsername());

        } catch (Exception e) {
            log.error("Failed to save user or assign roles. Rolling back Keycloak user {}", keycloakUserId, e);
            try {
                usersResource.get(keycloakUserId).remove();
            } catch (Exception ex) {
                log.error("Failed to rollback Keycloak user deletion for ID: {}", keycloakUserId, ex);
            }

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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "private_user_info", key = "#userId")
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

    @Override
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

    @Override
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

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "private_user_info", key = "#userId"),
            @CacheEvict(value = "users_owner", key = "#userId"),
            @CacheEvict(value = "public_user_info", key = "#updateUserDto.username")
    })
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

    @Override
    @Transactional
    @Cacheable(value = "public_user_info", key = "#username")
    public UserProfilePublicDto getPublicUserProfile(String username) {
        User user = findUser(username);
        return new UserProfilePublicDto(
                user.getId(),
                user.getUsername(),
                user.getGender().toString(),
                user.getBirthDate(),
                user.getAvatarFileName()
        );
    }


    private Set<Role> setUserRoles(UsersResource usersResource, boolean isAdmin, String keycloakUserId) {
        Set<Role> roles = new HashSet<>();

        Role defaultRole = roleRepository.findByRole(LOCAL_USER_ROLE)
                .orElseThrow(() -> new EntityNotFoundException("Role 'USER' not found"));

        roles.add(defaultRole);
        if (isAdmin) {
        RoleRepresentation roleAdmin = keycloak.realm(realm)
                .roles()
                .get(KEYCLOAK_ADMIN_ROLE)
                .toRepresentation();

        usersResource.get(keycloakUserId)
                .roles()
                .realmLevel()
                .add(List.of(roleAdmin));


        Role adminRole = roleRepository.findByRole(LOCAL_ADMIN_ROLE)
                .orElseThrow(() -> new EntityNotFoundException("Role 'ADMIN' not found"));

        roles.add(adminRole);
        }
        return roles;
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
                .orElseThrow(() -> new UsernameNotFoundException("Not found user with id: %s"
                        .formatted(userId)));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user with login: %s"
                        .formatted(username)));
    }

    private Set<String> mapRoleToString(Set<Role> roles) {
        return roles.stream()
                .map(Role::getRole)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());
    }
}