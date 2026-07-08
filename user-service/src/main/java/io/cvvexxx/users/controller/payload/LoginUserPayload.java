package io.cvvexxx.users.controller.payload;

public record LoginUserPayload(
        String username,
        String password
) {
}
