package io.cvvexxx.frontend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jwtUtils = new JwtUtils(objectMapper);
    }

    // Вспомогательный метод для сборки структурально валидного JWT
    private String createDummyJwtToken(String jsonPayload) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(jsonPayload.getBytes());
        return header + "." + payload + ".dummySignature";
    }

    @Nested
    @DisplayName("Тесты метода parsePayload")
    class ParsePayloadTests {

        @Test
        @DisplayName("Возвращает null при передаче null или пустой строки")
        void parsePayload_NullOrBlank_ReturnsNull() {
            assertNull(jwtUtils.parsePayload(null));
            assertNull(jwtUtils.parsePayload(""));
            assertNull(jwtUtils.parsePayload("   "));
        }

        @Test
        @DisplayName("Возвращает null для некорректного формата токена (без точек)")
        void parsePayload_InvalidFormat_ReturnsNull() {
            assertNull(jwtUtils.parsePayload("invalidTokenWithoutDots"));
        }

        @Test
        @DisplayName("Успешно парсит валидный JWT пейлоад")
        void parsePayload_ValidToken_ReturnsJsonNode() {
            String jwt = createDummyJwtToken("{\"sub\":\"12345\",\"name\":\"John Doe\"}");
            JsonNode payload = jwtUtils.parsePayload(jwt);

            assertNotNull(payload);
            assertEquals("12345", payload.path("sub").asText());
            assertEquals("John Doe", payload.path("name").asText());
        }

        @Test
        @DisplayName("Возвращает null при ошибке декодирования Base64 или парсинга JSON")
        void parsePayload_MalformedBase64_ReturnsNull() {
            assertNull(jwtUtils.parsePayload("header.!!!invalid-base64!!!.signature"));
        }
    }

    @Nested
    @DisplayName("Тесты метода extractUserId")
    class ExtractUserIdTests {

        @Test
        @DisplayName("Возвращает null, если payloadNode == null")
        void extractUserId_NullPayload_ReturnsNull() {
            assertNull(jwtUtils.extractUserId(null));
        }

        @Test
        @DisplayName("Возвращает null, если поле 'sub' отсутствует")
        void extractUserId_MissingSub_ReturnsNull() {
            JsonNode payload = objectMapper.createObjectNode();
            assertNull(jwtUtils.extractUserId(payload));
        }

        @Test
        @DisplayName("Возвращает null, если 'sub' не является валидным UUID")
        void extractUserId_InvalidUuid_ReturnsNull() {
            String jwt = createDummyJwtToken("{\"sub\":\"not-a-uuid\"}");
            JsonNode payload = jwtUtils.parsePayload(jwt);
            assertNull(jwtUtils.extractUserId(payload));
        }

        @Test
        @DisplayName("Успешно извлекает UUID из корректного поля 'sub'")
        void extractUserId_ValidUuid_ReturnsUUID() {
            UUID expectedId = UUID.randomUUID();
            String jwt = createDummyJwtToken("{\"sub\":\"" + expectedId + "\"}");
            JsonNode payload = jwtUtils.parsePayload(jwt);

            UUID actualId = jwtUtils.extractUserId(payload);
            assertEquals(expectedId, actualId);
        }
    }

    @Nested
    @DisplayName("Тесты метода extractAuthorities")
    class ExtractAuthoritiesTests {

        @Test
        @DisplayName("Возвращает пустой список, если payloadNode == null")
        void extractAuthorities_NullPayload_ReturnsEmptyList() {
            assertTrue(jwtUtils.extractAuthorities(null).isEmpty());
        }

        @Test
        @DisplayName("Возвращает пустой список, если realm_access отсутствует")
        void extractAuthorities_MissingRealmAccess_ReturnsEmptyList() {
            JsonNode payload = objectMapper.createObjectNode();
            assertTrue(jwtUtils.extractAuthorities(payload).isEmpty());
        }

        @Test
        @DisplayName("Корректно преобразует роли и добавляет префикс ROLE_, если его нет")
        void extractAuthorities_ValidRoles_AddsRolePrefix() {
            String json = """
                    {
                      "realm_access": {
                        "roles": ["user", "ROLE_admin"]
                      }
                    }
                    """;
            JsonNode payload = jwtUtils.parsePayload(createDummyJwtToken(json));

            List<GrantedAuthority> authorities = jwtUtils.extractAuthorities(payload);

            assertEquals(2, authorities.size());
            List<String> authorityNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
            assertTrue(authorityNames.contains("ROLE_user"));
            assertTrue(authorityNames.contains("ROLE_admin"));
        }
    }

    @Nested
    @DisplayName("Тесты метода isTokenExpired")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("Возвращает true, если токен невалиден или null")
        void isTokenExpired_InvalidToken_ReturnsTrue() {
            assertTrue(jwtUtils.isTokenExpired(null, 0));
            assertTrue(jwtUtils.isTokenExpired("invalid.token", 0));
        }

        @Test
        @DisplayName("Возвращает true, если время истечения 'exp' уже прошло")
        void isTokenExpired_ExpiredToken_ReturnsTrue() {
            long pastTime = (System.currentTimeMillis() / 1000) - 100;
            String jwt = createDummyJwtToken("{\"exp\":" + pastTime + "}");

            assertTrue(jwtUtils.isTokenExpired(jwt, 0));
        }

        @Test
        @DisplayName("Возвращает true, если токен истекает в пределах запаса времени (bufferSeconds)")
        void isTokenExpired_WithinBufferTime_ReturnsTrue() {
            long futureTime = (System.currentTimeMillis() / 1000) + 10; // Истечет через 10 сек
            String jwt = createDummyJwtToken("{\"exp\":" + futureTime + "}");

            // Буфер 30 сек: 10 секунд до истечения считаются просроченными
            assertTrue(jwtUtils.isTokenExpired(jwt, 30));
        }

        @Test
        @DisplayName("Возвращает false, если токен еще действителен")
        void isTokenExpired_ValidToken_ReturnsFalse() {
            long futureTime = (System.currentTimeMillis() / 1000) + 3600; // Истечет через час
            String jwt = createDummyJwtToken("{\"exp\":" + futureTime + "}");

            assertFalse(jwtUtils.isTokenExpired(jwt, 60));
        }
    }
}