package io.cvvexxx.users.controller;

import io.cvvexxx.users.dto.UserProductOwnerDto;
import io.cvvexxx.users.service.user.DefaultUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
@Slf4j
public class InternalUsersController {

    private final DefaultUserService userService;


    @GetMapping
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<List<UserProductOwnerDto>> getUserByIds(@RequestParam("ids") List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of());
        }

        return ResponseEntity.ok(userService.findUsersByIds(ids));
    }

    @GetMapping("{userId}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<UserProductOwnerDto> getUserById(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(userService.findUserById(userId));
    }

}
