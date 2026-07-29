package io.cvvexxx.frontend.dto.product;

import java.util.UUID;

public record ProductOwnerDto(
        UUID id,
        String username,
        String avatarFileName
) {
}
