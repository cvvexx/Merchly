package io.cvvexxx.users.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginUserDto(
        @NotBlank(message = "{user.login.required}")
        String login,

        @NotBlank(message = "{user.password.required}")
        String password
) {
}
