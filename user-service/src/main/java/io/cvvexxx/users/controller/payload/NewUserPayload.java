package io.cvvexxx.users.controller.payload;

public record NewUserPayload(
        String username,
        String password
) {
}
