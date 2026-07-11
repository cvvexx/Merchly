package io.cvvexxx.users.controller;

import io.cvvexxx.users.controller.payload.LoginUserPayload;
import io.cvvexxx.users.dto.JwtAuthenticationDto;
import io.cvvexxx.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
public class UsersController {

    private final UserService userService;

    @PostMapping("auth")
    public ResponseEntity<JwtAuthenticationDto> registerUser(@RequestBody LoginUserPayload payload) {
        JwtAuthenticationDto jwtDto = userService.registerUser(payload.username(), payload.password());
        return ResponseEntity.ok(jwtDto);
    }

    @PostMapping("login")
    public ResponseEntity<JwtAuthenticationDto> loginUser(@RequestBody LoginUserPayload payload) {
        JwtAuthenticationDto jwtDto = userService.loginUser(payload.username(), payload.password());
        return ResponseEntity.ok(jwtDto);
    }
}