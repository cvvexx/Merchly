package io.cvvexxx.frontend.dto;

import java.util.List;

public record UserDto(
        int id,
        String username,
        List<String> roles
) {
}
