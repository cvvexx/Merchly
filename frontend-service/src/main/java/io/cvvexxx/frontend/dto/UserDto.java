package io.cvvexxx.frontend.dto;

import java.util.Set;

public record UserDto(
        int id,
        String username,
        Set<String> roles
) {
}
