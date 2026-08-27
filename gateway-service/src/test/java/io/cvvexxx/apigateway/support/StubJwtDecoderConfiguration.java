package io.cvvexxx.apigateway.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

@TestConfiguration
public class StubJwtDecoderConfiguration {

    @Bean
    public JwtDecoder jwtDecoder() {
        ObjectMapper objectMapper = new ObjectMapper();

        return token -> {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                throw new BadJwtException("Не похоже на JWT: " + token);
            }

            Map<String, Object> claims;
            try {
                claims = objectMapper.readValue(
                        Base64.getUrlDecoder().decode(chunks[1]),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        });
            } catch (Exception e) {
                throw new BadJwtException("Не удалось разобрать payload токена", e);
            }

            Instant now = Instant.now();
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claims(target -> target.putAll(claims))
                    .issuedAt(now)
                    .expiresAt(now.plus(1, ChronoUnit.HOURS))
                    .build();
        };
    }
}
