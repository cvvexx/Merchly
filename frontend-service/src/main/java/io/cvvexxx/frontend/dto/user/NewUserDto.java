package io.cvvexxx.frontend.dto.user;

import java.time.LocalDate;

public record NewUserDto(
        String firstName,
        String lastName,
        String username,
        String password,
        String email,
        String gender,
        LocalDate birthDate
) {
}
