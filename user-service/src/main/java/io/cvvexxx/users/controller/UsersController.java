package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.UserInfoDto;
import io.cvvexxx.users.security.SecurityUser;
import io.cvvexxx.users.security.dto.JwtAuthenticationDto;
import io.cvvexxx.users.security.dto.LoginUserDto;
import io.cvvexxx.users.security.dto.NewUserDto;
import io.cvvexxx.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
public class UsersController {

    private final UserService userService;

    @PostMapping("auth")
    public ResponseEntity<JwtAuthenticationDto> registerUser(
            @Valid @RequestBody NewUserDto newUserDto,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
            JwtAuthenticationDto jwtDto = userService.registerUser(newUserDto);

            return ResponseEntity.ok(jwtDto);
        }
    }

    @PostMapping("login")
    public ResponseEntity<JwtAuthenticationDto> loginUser(
            @Valid @RequestBody LoginUserDto loginUserDto,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
            JwtAuthenticationDto jwtDto = userService.loginUser(loginUserDto);

            return ResponseEntity.ok(jwtDto);
        }
    }

    @GetMapping("me")
    public ResponseEntity<UserInfoDto> getSecurityUserInfo(@AuthenticationPrincipal SecurityUser securityUser) {
        UserInfoDto currentUserInfo = userService.getUserInfo(securityUser.getUsername());

        return ResponseEntity.ok(currentUserInfo);
    }
}