package io.cvvexxx.users.service;


import io.cvvexxx.users.dto.UserDto;
import io.cvvexxx.users.entity.Role;
import io.cvvexxx.users.entity.User;
import io.cvvexxx.users.repository.RoleRepository;
import io.cvvexxx.users.repository.UserRepository;
import io.cvvexxx.users.security.DefaultUserDetailsService;
import io.cvvexxx.users.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final RoleRepository roleRepository; //TODO(КАКАЯ ТО ХУЕТА)
    private final UserRepository userRepository;
    private final DefaultUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public UserDto authUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже есть");//TODO
        }

        String hashPassword = passwordEncoder.encode(password);

        Role role = roleRepository.findByRole("USER");

        User user = new User(
                null,
                username,
                hashPassword,
                Set.of(role)
        );

        logger.info("User {}", user);

        return mapToDto(
                userRepository.save(
                    user
                )
        );
    }

    public UserDto loginUser(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityUser securityUser = (SecurityUser) userDetailsService.loadUserByUsername(username);

        return mapToDto(securityUser.getUser());
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(Role::getRole)
                        .map(role -> "ROLE_" + role)
                        .toList()
        );
    }

}
