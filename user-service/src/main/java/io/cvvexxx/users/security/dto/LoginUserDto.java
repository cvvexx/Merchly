package io.cvvexxx.users.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginUserDto(
        @NotBlank(message = "{user.login.required}")
        @JsonProperty("login")
        String login,

        @NotBlank(message = "{user.password.required}")
        @JsonProperty("password")
        String password
) {
}
