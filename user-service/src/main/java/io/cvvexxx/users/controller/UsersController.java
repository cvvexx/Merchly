package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.UserInfoDto;
import io.cvvexxx.users.security.SecurityUser;
import io.cvvexxx.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Slf4j
public class UsersController {

    private final UserService userService;

    @GetMapping("me")
    public ResponseEntity<UserInfoDto> getSecurityUserInfo(@AuthenticationPrincipal Jwt jwt) {
        UserInfoDto currentUserInfo = userService.getUserInfo(jwt.getClaimAsString("preferred_username"));

        return ResponseEntity.ok(currentUserInfo);
    }
}