package io.cvvexxx.users.dto;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record UserInfoDto(
        String username,
        String email,
        String gender,
        LocalDate birthDate,
        Set<String> roles
) {
}
