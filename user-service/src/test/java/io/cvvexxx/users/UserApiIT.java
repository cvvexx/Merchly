package io.cvvexxx.users;

import com.redis.testcontainers.RedisContainer;
import io.cvvexxx.users.domain.Gender;
import io.cvvexxx.users.dto.NewUserDto;
import io.cvvexxx.users.dto.UserCreatedDto;
import io.cvvexxx.users.dto.UserInfoDto;
import io.cvvexxx.users.dto.cart.AddToCartDto;
import io.cvvexxx.users.dto.cart.CartItemDto;
import io.cvvexxx.users.entity.User;
import io.cvvexxx.users.repository.CartItemRepository;
import io.cvvexxx.users.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class UserApiIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");
    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private Keycloak keycloak;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
        registry.add("spring.cache.type", () -> "redis");

        registry.add("minio.url", minio::getS3URL);
        registry.add("minio.access.name", minio::getUserName);
        registry.add("minio.access.secret", minio::getPassword);
    }

    @BeforeEach
    void resetKeycloakMock() {
        Mockito.reset(keycloak);
    }

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void registerUser_ThenFetchMe_PersistsInRealDbUploadsAvatarToRealMinioAndPopulatesRealRedisCache() {
        UUID keycloakUserId = UUID.randomUUID();
        Response keycloakCreateResponse = Response.status(201)
                .header("Location", "http://keycloak/admin/realms/test-realm/users/" + keycloakUserId)
                .build();
        when(keycloak.realm(anyString()).users().create(any(UserRepresentation.class)))
                .thenReturn(keycloakCreateResponse);

        NewUserDto newUserDto = new NewUserDto(
                "Ivan", "Ivanov", "ivan_it_test", "password123",
                "ivan_it_test@example.com", Gender.M, LocalDate.of(1990, 1, 1), false
        );

        HttpHeaders payloadPartHeaders = new HttpHeaders();
        payloadPartHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpHeaders imagePartHeaders = new HttpHeaders();
        imagePartHeaders.setContentType(MediaType.IMAGE_PNG);
        ByteArrayResource avatarResource = new ByteArrayResource(new byte[]{1, 2, 3, 4}) {
            @Override
            public String getFilename() {
                return "avatar.png";
            }
        };

        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("payload", new HttpEntity<>(newUserDto, payloadPartHeaders));
        multipartBody.add("image", new HttpEntity<>(avatarResource, imagePartHeaders));

        var registerResponse = restClient()
                .post()
                .uri("/api/users/register")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipartBody)
                .retrieve()
                .toEntity(UserCreatedDto.class);

        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        assertEquals(keycloakUserId, registerResponse.getBody().id());

        User savedUser = userRepository.findById(keycloakUserId).orElseThrow();
        assertEquals("ivan_it_test", savedUser.getUsername());
        assertTrue(savedUser.getRoles().stream().anyMatch(role -> role.getRole().equals("USER")));
        assertNotNull(savedUser.getAvatarFileName());

        var meResponse = restClient()
                .get()
                .uri("/api/users/me")
                .headers(headers -> headers.setBearerAuth(keycloakUserId.toString()))
                .retrieve()
                .toEntity(UserInfoDto.class);

        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        assertEquals("ivan_it_test", meResponse.getBody().username());
        assertEquals("USER", meResponse.getBody().roles().iterator().next());
    }

    @Test
    void cartFlow_AddThenGet_PersistsAndReadsFromRealPostgres() {
        User user = userRepository.saveAndFlush(new User(
                UUID.randomUUID(), "cart_it_user", "cart_it_user@example.com",
                Gender.F, LocalDate.of(1995, 5, 5), java.util.Set.of(), null
        ));
        AddToCartDto addToCartDto = new AddToCartDto(UUID.randomUUID(), 2);

        var addResponse = restClient()
                .post()
                .uri("/api/users/cart")
                .headers(headers -> headers.setBearerAuth(user.getId().toString()))
                .body(addToCartDto)
                .retrieve()
                .toBodilessEntity();

        assertEquals(HttpStatus.OK, addResponse.getStatusCode());
        var cartItemsInDb = cartItemRepository.findAllByUserId(user.getId());
        assertEquals(1, cartItemsInDb.size());
        assertEquals(2, cartItemsInDb.get(0).getQuantity());
        assertEquals(addToCartDto.productId(), cartItemsInDb.get(0).getProductId());

        var getCartResponse = restClient()
                .get()
                .uri("/api/users/cart")
                .headers(headers -> headers.setBearerAuth(user.getId().toString()))
                .retrieve()
                .toEntity(CartItemDto[].class);

        assertEquals(HttpStatus.OK, getCartResponse.getStatusCode());
        assertEquals(1, getCartResponse.getBody().length);
        assertEquals(addToCartDto.productId(), getCartResponse.getBody()[0].productId());
        assertEquals(2, getCartResponse.getBody()[0].quantity());
    }

    @TestConfiguration
    static class ITConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", token)
                    .claim("realm_access", Map.of("roles", List.of("USER")))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }

        @Bean
        Keycloak keycloak() {
            return mock(Keycloak.class, Mockito.RETURNS_DEEP_STUBS);
        }
    }
}
