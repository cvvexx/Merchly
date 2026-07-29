package io.cvvexxx.users.dto;

import io.cvvexxx.users.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record NewUserDto(

        @Size(min = 2, max = 255, message = "{user.create.firstName.size}")
        @NotNull(message = "{user.create.firstName.required}")
        String firstName,

        @Size(min = 2, max = 255, message = "{user.create.lastName.size}")
        @NotNull(message = "{user.create.lastName.required}")
        String lastName,

        @Size(min = 3, max = 255, message = "{user.create.username.size}")
        @NotNull(message = "{user.create.username.required}")
        String username,

        @Size(min = 8, max = 255, message = "{user.create.password.length}")
        @NotNull(message = "{user.create.password.required}")
        String password,

        @NotNull(message = "{user.create.email.required}")
        @Email(message = "{user.create.email.invalid}")
        String email,

        @NotNull(message = "{user.create.gender.required}")
        Gender gender,

        @NotNull(message = "{user.create.birthdate.required}")
        @Past(message = "{user.create.birthdate.past}")
        LocalDate birthDate
) {
}
