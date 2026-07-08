package io.cvvexxx.users.controller;


import io.cvvexxx.users.controller.payload.LoginUserPayload;
import io.cvvexxx.users.dto.UserDto;
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
    public ResponseEntity<UserDto> authUser(@RequestBody LoginUserPayload payload) {
        UserDto userDto = userService.authUser(payload.username(), payload.username());

        return ResponseEntity.ok(userDto);
    }

    @PostMapping("login")
    public ResponseEntity<UserDto> loginUser(@RequestBody LoginUserPayload payload) {
        UserDto userDto = userService.loginUser(payload.username(), payload.password());

        return ResponseEntity.ok(userDto);
    }


}
