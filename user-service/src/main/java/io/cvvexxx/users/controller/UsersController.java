package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.*;
import io.cvvexxx.users.service.user.DefaultUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Slf4j
public class UsersController {

    private final DefaultUserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserCreatedDto> registerUser(
            @Valid @RequestPart("payload") NewUserDto newUserDto,
            @RequestPart(value = "image", required = false) MultipartFile userAvatar,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
            log.info("New user registration request received username {}", newUserDto.username());
            return ResponseEntity.ok(userService.registerUserInKeycloakAndLocalDb(newUserDto, userAvatar));
        }
    }

    @GetMapping("me")
    public ResponseEntity<UserInfoDto> getSecurityUserInfo(@AuthenticationPrincipal Jwt jwt) {
        UserInfoDto currentUserInfo = userService.getUserInfo(UUID.fromString(jwt.getClaimAsString("sub")));
        return ResponseEntity.ok(currentUserInfo);
    }

    @PostMapping("edit")
    public ResponseEntity<Void> updateUserInfo(
            @Valid @RequestPart("payload") UpdateUserDto updateUserDto,
            @RequestPart(value = "image", required = false) MultipartFile userAvatar,
            @AuthenticationPrincipal Jwt jwt,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
            UUID currentUserId = UUID.fromString(jwt.getClaimAsString("sub"));

            userService.updateUserInfo(currentUserId, updateUserDto, userAvatar);
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("{username}")
    public ResponseEntity<UserProfilePublicDto> getPublicUserProfile(@PathVariable("username") String username) {
        UserProfilePublicDto profile = userService.getPublicUserProfile(username);
        return ResponseEntity.ok(profile);
    }
}