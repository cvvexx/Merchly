package io.cvvexxx.frontend.dto.user;

import java.util.Set;

public record UserDto(
        int id,
        String username,
        Set<String> roles,
        String userAvatarUrl
) {
}
