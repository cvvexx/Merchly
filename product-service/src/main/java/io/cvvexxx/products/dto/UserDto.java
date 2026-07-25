package io.cvvexxx.products.dto;

import java.util.Set;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        Set<String> roles
) {
}
