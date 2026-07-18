package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.UserProductOwnerDto;
import io.cvvexxx.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
public class InternalUsersController {

    private final UserService userService;

    @GetMapping//TODO(Решить проблему безопасности)
    //PreAuthorize(hasRole(Service))
    public ResponseEntity<List<UserProductOwnerDto>> getUserByIds(@RequestParam("ids") List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of());
        }

        return ResponseEntity.ok(userService.findUsersByIds(ids));
    }

    @GetMapping("{userId}")
    public ResponseEntity<UserProductOwnerDto> getUserById(@PathVariable("userId") Integer userId) {
        return ResponseEntity.ok(userService.findUserById(userId));
    }

}
