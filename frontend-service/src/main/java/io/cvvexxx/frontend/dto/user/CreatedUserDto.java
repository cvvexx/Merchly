package io.cvvexxx.frontend.dto.user;

import java.util.UUID;

public record CreatedUserDto(
        UUID id,
        String username
) {
}
