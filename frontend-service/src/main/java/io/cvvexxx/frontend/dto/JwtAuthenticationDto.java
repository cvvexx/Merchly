package io.cvvexxx.frontend.dto;

public record JwtAuthenticationDto(
        String token,
        String refreshToken
) {
}
