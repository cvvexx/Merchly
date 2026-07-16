package io.cvvexxx.users.service;

import io.cvvexxx.users.dto.JwtAuthenticationDto;
import io.cvvexxx.users.dto.UserDto;
import io.cvvexxx.users.entity.Role;
import io.cvvexxx.users.entity.User;
import io.cvvexxx.users.repository.RoleRepository;
import io.cvvexxx.users.repository.UserRepository;
import io.cvvexxx.users.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    private final JwtService jwtService;

    @Transactional
    public JwtAuthenticationDto registerUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        String hashPassword = passwordEncoder.encode(password);
        Role role = roleRepository.findByRole("USER");
        Set<Role> roles = Set.of(role);
        User user = new User(null, username, hashPassword, Set.of(role));
        User savedUser = userRepository.save(user);
        logger.info("New user registered: {}", username);


        return jwtService.generateAuthToken(
                savedUser.getId(), username, mapRoleToString(roles)
        );
    }

    @Transactional
    public JwtAuthenticationDto loginUser(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        User user = findUser(username);

        Set<String> roles = mapRoleToString(user.getRoles());
        return jwtService.generateAuthToken(user.getId(), username, roles);
    }

    @Transactional(readOnly = true)
    public UserDto getUserInfo(String username) {
        User user = findUser(username);

        return new UserDto(
                user.getId(),
                user.getUsername(),
                mapRoleToString(user.getRoles())
        );
    }
    @Transactional(readOnly = true)
    public Set<String> getUserRoles(String username) {
        User user = findUser(username);

        return mapRoleToString(user.getRoles());
    }

    protected User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user with username: %s"
                        .formatted(username)));
    }

    private Set<String> mapRoleToString(Set<Role> roles) {
        return roles.stream()
                .map(Role::getRole)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());
    }
}