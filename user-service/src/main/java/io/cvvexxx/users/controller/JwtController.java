package io.cvvexxx.users.controller;


import io.cvvexxx.users.dto.JwtAuthenticationDto;
import io.cvvexxx.users.security.jwt.JwtService;
import io.cvvexxx.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RequiredArgsConstructor
@RequestMapping("api/jwt")
@RestController
public class JwtController {

    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is missing");
        }
        try {
            var usernameOpt = jwtService.getUsernameFromToken(refreshToken);
            if (usernameOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
            }
            String username = usernameOpt.get();
            Set<String> roles = userService.getUserRoles(username);
            JwtAuthenticationDto newTokens = jwtService.refreshBaseToken(username, roles, refreshToken);

            return ResponseEntity.ok(newTokens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error during token refresh");
        }
    }
}
