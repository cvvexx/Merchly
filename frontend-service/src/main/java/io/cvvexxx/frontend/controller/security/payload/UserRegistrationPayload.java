package io.cvvexxx.frontend.controller.security.payload;

public record UserRegistrationPayload(
        String username,
        String password
) {
}
