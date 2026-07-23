package io.cvvexxx.users.dto;

import java.util.UUID;

public record UserCreatedDto(
        UUID id,
        String username
) {
}
