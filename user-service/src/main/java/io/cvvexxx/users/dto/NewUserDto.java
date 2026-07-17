package io.cvvexxx.users.dto;

import io.cvvexxx.users.domain.Gender;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record NewUserDto(
        @NotBlank(message = "{user.username.required}")
        @Size(min = 3, max = 30, message = "{user.username.size}")
        String username,

        @NotBlank(message = "{user.password.required}")
        @Size(min = 8, max = 255, message = "{user.password.length}")
        String password,

        @NotBlank(message = "{user.email.required}")
        @Email(message = "{user.email.invalid}")
        String email,

        @NotNull(message = "{user.gender.required}")
        Gender gender,

        @NotNull(message = "{user.birthdate.required}")
        @Past(message = "{user.birthdate.past}")
        LocalDate birthDate
) {}
