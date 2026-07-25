package io.cvvexxx.frontend.dto;


import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record UserInfoDto(
        UUID id,
        String username,
        String email,
        String gender,
        LocalDate birthDate,
        Set<String> roles
) {
}
