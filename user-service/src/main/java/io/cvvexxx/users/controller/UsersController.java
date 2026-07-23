package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.NewUserDto;
import io.cvvexxx.users.dto.UserCreatedDto;
import io.cvvexxx.users.dto.UserInfoDto;
import io.cvvexxx.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Slf4j
public class UsersController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserCreatedDto> registerUser(@Valid @RequestBody NewUserDto newUserDto) {
        log.info("Received request to register user {}", newUserDto);
        return ResponseEntity.ok(userService.registerUserInKeycloakAndLocalDb(newUserDto));
    }

    @GetMapping("me")
    public ResponseEntity<UserInfoDto> getSecurityUserInfo(@AuthenticationPrincipal Jwt jwt) {
        UserInfoDto currentUserInfo = userService.getUserInfo(jwt.getClaimAsString("preferred_username"));

        return ResponseEntity.ok(currentUserInfo);
    }
}