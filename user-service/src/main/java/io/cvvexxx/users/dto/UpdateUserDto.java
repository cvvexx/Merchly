package io.cvvexxx.users.dto;

import io.cvvexxx.users.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserDto(

        @NotNull(message = "{user.update.username.required}")
        @Size(min = 3, max = 255, message = "{user.update.username.size}")
        String username,

        @Email(message = "{user.update.email.invalid}")
        @NotNull(message = "{user.update.email.required}")
        String email,

        @NotNull(message = "{user.update.gender.required}")
        Gender gender,

        @NotNull(message = "{user.update.birthdate.required}")
        @Past(message = "{user.update.birthdate.past}")
        LocalDate birthDate
) {
}