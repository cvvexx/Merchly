package io.cvvexxx.users.dto;

import java.time.LocalDate;

public record UpdateUserDto(
        String username,
        String email,
        String gender,
        LocalDate birthDate
) {
}