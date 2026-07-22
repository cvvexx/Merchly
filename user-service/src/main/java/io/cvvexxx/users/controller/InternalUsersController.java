package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.UserProductOwnerDto;
import io.cvvexxx.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasRole;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
public class InternalUsersController {

    private final UserService userService;

    @GetMapping//TODO(Решить проблему безопасности)
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<List<UserProductOwnerDto>> getUserByIds(@RequestParam("ids") List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of());
        }

        return ResponseEntity.ok(userService.findUsersByIds(ids));
    }

    @GetMapping("{userId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<UserProductOwnerDto> getUserById(@PathVariable("userId") Integer userId) {
        return ResponseEntity.ok(userService.findUserById(userId));
    }

}
