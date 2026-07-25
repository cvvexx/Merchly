package io.cvvexxx.frontend.dto;

import java.util.UUID;

public record CreatedUserDto(
        UUID id,
        String username
) {
}
