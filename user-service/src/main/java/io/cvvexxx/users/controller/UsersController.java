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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Slf4j
public class UsersController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserCreatedDto> registerUser(
            @Valid @RequestPart("payload") NewUserDto newUserDto,
            @RequestPart(value = "image", required = false) MultipartFile userAvatar
    ) {
        log.info("Received request to register user {}", newUserDto);
        log.info("userAvatar: {}", userAvatar);
        return ResponseEntity.ok(userService.registerUserInKeycloakAndLocalDb(newUserDto, userAvatar));
    }

    @GetMapping("me")
    public ResponseEntity<UserInfoDto> getSecurityUserInfo(@AuthenticationPrincipal Jwt jwt) {
        UserInfoDto currentUserInfo = userService.getUserInfo(jwt.getClaimAsString("preferred_username"));
        log.info("currentUserInfo: {}", currentUserInfo);
        return ResponseEntity.ok(currentUserInfo);
    }
}