package io.cvvexxx.frontend.controller.security.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserLoginPayload(
        @JsonProperty("login")
        String login,

        @JsonProperty("password")
        String password
) {
}
