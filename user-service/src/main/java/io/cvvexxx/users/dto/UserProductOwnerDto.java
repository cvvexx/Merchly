package io.cvvexxx.users.dto;

import java.util.UUID;

public record UserProductOwnerDto(
        UUID id,
        String username,
        String avatarFileName
) {
}
