package io.cvvexxx.frontend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtils {

    private final ObjectMapper objectMapper;

    public JsonNode parsePayload(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(chunks[1]));
            return objectMapper.readTree(payloadJson);
        } catch (Exception e) {
            log.error("Failed to parse JWT payload", e);
            return null;
        }
    }

    public UUID extractUserId(JsonNode payloadNode) {
        if (payloadNode == null) return null;
        try {
            String sub = payloadNode.path("sub").asText(null);
            return sub != null ? UUID.fromString(sub) : null;
        } catch (Exception e) {
            log.error("Failed to parse 'sub' (userId) from token", e);
            return null;
        }
    }

    public List<GrantedAuthority> extractAuthorities(JsonNode payloadNode) {
        if (payloadNode == null) return Collections.emptyList();

        try {
            JsonNode realmAccessNode = payloadNode.path("realm_access").path("roles");
            List<GrantedAuthority> authorities = new ArrayList<>();

            if (realmAccessNode.isArray()) {
                for (JsonNode roleNode : realmAccessNode) {
                    String roleName = roleNode.asText();
                    String authorityName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    authorities.add(new SimpleGrantedAuthority(authorityName));
                }
            }
            return authorities;
        } catch (Exception e) {
            log.error("Failed to parse roles from JWT payload", e);
            return Collections.emptyList();
        }
    }

    public boolean isTokenExpired(String accessToken, long bufferSeconds) {
        JsonNode payload = parsePayload(accessToken);
        if (payload == null) return true;

        try {
            long exp = payload.path("exp").asLong();
            long currentTime = System.currentTimeMillis() / 1000;
            return exp <= (currentTime + bufferSeconds);
        } catch (Exception e) {
            return true;
        }
    }
}