package io.cvvexxx.frontend.controller.security.payload;

import java.time.LocalDate;

public record NewUserPayload(
        String username,
        String password,
        String email,
        String gender,
        LocalDate birthDate

) {
}
