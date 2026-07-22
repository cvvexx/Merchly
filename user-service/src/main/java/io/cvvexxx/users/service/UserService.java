package io.cvvexxx.users.service;

import io.cvvexxx.users.dto.UserInfoDto;
import io.cvvexxx.users.dto.UserProductOwnerDto;
import io.cvvexxx.users.entity.Role;
import io.cvvexxx.users.entity.User;
import io.cvvexxx.users.repository.RoleRepository;
import io.cvvexxx.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;


    @Transactional(readOnly = true)
    @Cacheable(value = "user_info", key = "#username")
    public UserInfoDto getUserInfo(String username) {
        User user = findUser(username);

        return new UserInfoDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getGender().name(),
                user.getBirthDate(),
                mapRoleToString(user.getRoles())
        );
    }

    @Transactional(readOnly = true)
    public Set<String> getUserRoles(String username) {
        User user = findUser(username);

        return mapRoleToString(user.getRoles());
    }

    protected User findUser(String login) {
        return userRepository.findByUsernameOrEmail(login, login)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user with login: %s"
                        .formatted(login)));
    }

    protected User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user with login: %d"
                        .formatted(userId)));
    }

    private Set<String> mapRoleToString(Set<Role> roles) {
        return roles.stream()
                .map(Role::getRole)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<UserProductOwnerDto> findUsersByIds(List<Integer> ids) {

        List<Integer> validIds = ids.stream()
                .filter(id -> id != null && id > 0)
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
    public UserProductOwnerDto findUserById(Integer userId) {

        if (userId == null || userId <= 0) {
            return new UserProductOwnerDto(0, "Неизвестен");
        }
        User user = findUser(userId);

        return new UserProductOwnerDto(
                user.getId(),
                user.getUsername()
        );
    }
}