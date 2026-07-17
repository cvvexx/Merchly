package io.cvvexxx.users.service;

import io.cvvexxx.users.security.dto.JwtAuthenticationDto;
import io.cvvexxx.users.security.dto.LoginUserDto;
import io.cvvexxx.users.security.dto.NewUserDto;
import io.cvvexxx.users.dto.CurrentUserInfo;
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

import java.util.HashSet;
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
    public JwtAuthenticationDto registerUser(NewUserDto newUserDto) {
        if (userRepository.existsByUsername(newUserDto.username())) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        if (userRepository.existsByEmail(newUserDto.email())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        String hashPassword = passwordEncoder.encode(newUserDto.password());
        Role role = roleRepository.findByRole("USER");
        Set<Role> roles = new HashSet<>(Set.of(role));
        User user = new User(
                null,
                newUserDto.username(),
                hashPassword,
                newUserDto.email(),
                newUserDto.gender(),
                newUserDto.birthDate(),
                roles
        );

        User savedUser = userRepository.save(user);
        logger.info("New user registered: {}", newUserDto.username());


        return jwtService.generateAuthToken(
                savedUser.getId(), newUserDto.username(), mapRoleToString(roles)
        );
    }

    @Transactional
    public JwtAuthenticationDto loginUser(LoginUserDto loginUserDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginUserDto.login(), loginUserDto.password())
        );
        User user = findUser(loginUserDto.login());

        Set<String> roles = mapRoleToString(user.getRoles());
        return jwtService.generateAuthToken(user.getId(), user.getUsername(), roles);
    }

    @Transactional(readOnly = true)
    public CurrentUserInfo getUserInfo(String username) {
        User user = findUser(username);

        return new CurrentUserInfo(
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

    private Set<String> mapRoleToString(Set<Role> roles) {
        return roles.stream()
                .map(Role::getRole)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());
    }
}