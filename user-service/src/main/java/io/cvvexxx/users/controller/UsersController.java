package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.JwtAuthenticationDto;
import io.cvvexxx.users.dto.LoginUserDto;
import io.cvvexxx.users.dto.NewUserDto;
import io.cvvexxx.users.dto.UserDto;
import io.cvvexxx.users.security.SecurityUser;
import io.cvvexxx.users.service.UserService;
import jakarta.validation.Valid;
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
    public ResponseEntity<JwtAuthenticationDto> registerUser(@Valid @RequestBody NewUserDto newUserDto) {
        JwtAuthenticationDto jwtDto = userService.registerUser(newUserDto);

        return ResponseEntity.ok(jwtDto);
    }

    @PostMapping("login")
    public ResponseEntity<JwtAuthenticationDto> loginUser(@RequestBody LoginUserDto loginUserDto) {
        JwtAuthenticationDto jwtDto = userService.loginUser(loginUserDto);

        return ResponseEntity.ok(jwtDto);
    }

    @GetMapping("me")
    public ResponseEntity<UserDto> getUserInformation(@AuthenticationPrincipal SecurityUser securityUser) {
        UserDto userDto = userService.getUserInfo(securityUser.getUsername());

        return ResponseEntity.ok(userDto);
    }
}