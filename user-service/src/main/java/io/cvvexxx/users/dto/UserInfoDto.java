package io.cvvexxx.users.dto;

import java.time.LocalDate;
import java.util.Set;

public record UserInfoDto(
        String username,
        String email,
        String gender,
        LocalDate birthDate,
        Set<String> roles
) {
}
