package io.cvvexxx.users.controller;

import io.cvvexxx.users.controller.payload.LoginUserPayload;
import io.cvvexxx.users.controller.payload.NewUserPayload;
import io.cvvexxx.users.dto.JwtAuthenticationDto;
import io.cvvexxx.users.dto.UserDto;
import io.cvvexxx.users.security.SecurityUser;
import io.cvvexxx.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
public class UsersController {

    private final UserService userService;

    @PostMapping("auth")
    public ResponseEntity<JwtAuthenticationDto> registerUser(@RequestBody NewUserPayload payload) {
        JwtAuthenticationDto jwtDto = userService.registerUser(payload.username(), payload.password());

        return ResponseEntity.ok(jwtDto);
    }

    @PostMapping("login")
    public ResponseEntity<JwtAuthenticationDto> loginUser(@RequestBody LoginUserPayload payload) {
        JwtAuthenticationDto jwtDto = userService.loginUser(payload.username(), payload.password());

        return ResponseEntity.ok(jwtDto);
    }

    @GetMapping("me")
    public ResponseEntity<UserDto> getUserInformation(@AuthenticationPrincipal SecurityUser securityUser) {
        UserDto userDto = userService.getUserInfo(securityUser.getUsername());

        return ResponseEntity.ok(userDto);
    }
}