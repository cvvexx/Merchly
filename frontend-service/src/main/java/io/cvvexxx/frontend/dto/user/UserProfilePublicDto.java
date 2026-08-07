package io.cvvexxx.frontend.dto.user;

import java.time.LocalDate;
import java.util.UUID;

public record UserProfilePublicDto(
        UUID id,
        String username,
        String gender,
        LocalDate birthDate,
        String userAvatarUrl
) {
}