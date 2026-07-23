package io.cvvexxx.users.dto;

import io.cvvexxx.users.domain.Gender;

import java.time.LocalDate;

public record NewUserDto(
        String username,
        String password,
        String email,
        Gender gender,
        LocalDate birthDate
) {
}
