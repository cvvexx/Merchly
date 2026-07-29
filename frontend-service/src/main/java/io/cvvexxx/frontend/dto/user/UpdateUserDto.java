package io.cvvexxx.frontend.dto.user;

import java.time.LocalDate;

public record UpdateUserDto(
        String username,
        String email,
        String gender,
        LocalDate birthDate
) {
}
