package io.cvvexxx.frontend.controller.security.payload;

public record UserLoginPayload(
        String username,
        String password
) {
}
