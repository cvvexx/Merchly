package io.cvvexxx.apigateway.support;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class TestTokens {

    private TestTokens() {
    }

    public static String admin() {
        return withRealmRoles("ADMIN", "USER");
    }

    public static String user() {
        return withRealmRoles("USER");
    }

    public static String withRealmRoles(String... roles) {
        String rolesJson = List.of(roles).stream()
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(","));

        long expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();

        String payload = "{\"sub\":\"" + UUID.randomUUID()
                + "\",\"preferred_username\":\"tester\""
                + ",\"exp\":" + expiresAt
                + ",\"realm_access\":{\"roles\":[" + rolesJson + "]}}";

        return base64("{\"alg\":\"none\"}") + "." + base64(payload) + ".signature";
    }

    private static String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes());
    }
}
