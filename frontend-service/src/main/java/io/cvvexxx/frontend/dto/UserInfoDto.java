package io.cvvexxx.frontend.dto;


import java.time.LocalDate;
import java.util.Set;

public record UserInfoDto(
        int id,
        String username,
        String email,
        String gender,
        LocalDate birthDate,
        Set<String> roles
) {
}
