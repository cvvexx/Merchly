package io.cvvexxx.users.service;

import io.cvvexxx.users.dto.NewUserDto;
import io.cvvexxx.users.dto.UserCreatedDto;
import io.cvvexxx.users.dto.UserInfoDto;
import io.cvvexxx.users.dto.UserProductOwnerDto;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${keycloak.realm}")
    private String realm;

    @Transactional
    public UserCreatedDto registerUserInKeycloakAndLocalDb(NewUserDto newUserDto) {
        log.info("Received request to register user in keycloak and local db {}", newUserDto);

        UserRepresentation user = getUserRepresentation(newUserDto);

        UsersResource usersResource = keycloak.realm(realm).users();
        try (Response response = usersResource.create(user)) {
            log.info("Keycloak response status: {}", response.getStatus());

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
            }

            String locationHeader = response.getHeaderString("Location");
            if (locationHeader == null || locationHeader.isBlank()) {
                throw new IllegalStateException("Keycloak response did not include a Location header");
            }

            String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
            log.info("Keycloak User ID {}", keycloakUserId);

            Set<Role> roles = Set.of(roleRepository.findByRole("USER"));

            User localUser = new User(
                    UUID.fromString(keycloakUserId),
                    newUserDto.username(),
                    newUserDto.email(),
                    newUserDto.gender(),
                    newUserDto.birthDate(),
                    roles
            );

            User savedUser = userRepository.save(localUser);
            log.info("Saved user {}", savedUser);

            return new UserCreatedDto(
                    savedUser.getId(),
                    savedUser.getUsername()
            );
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
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
    @Cacheable(value = "user_info", key = "#username")
    public UserInfoDto getUserInfo(String username) {
        User user = findUser(username);

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
                cleanRoles
        );
    }


    protected User findUser(String login) {
        return userRepository.findByUsernameOrEmail(login, login)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user with login: %s"
                        .formatted(login)));
    }

    protected User findUser(UUID userId) {
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
                        user.getUsername()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users_owner", key = "#userId")
    public UserProductOwnerDto findUserById(UUID userId) {

        if (userId == null) {
            return new UserProductOwnerDto(null, "Неизвестен");
        }
        User user = findUser(userId);

        return new UserProductOwnerDto(
                user.getId(),
                user.getUsername()
        );
    }
}