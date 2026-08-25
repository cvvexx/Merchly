package io.cvvexxx.configserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTests {

    @Autowired
    private RestClient.Builder restClientBuilder;

    @LocalServerPort
    private int port;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = restClientBuilder.baseUrl("http://localhost:" + port).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("отдаёт свойства сервиса по /{application}/{profile}")
    void servesConfigForKnownApplication() {
        Environment env = client.get()
                .uri("/user-service/default")
                .retrieve()
                .body(Environment.class);

        assertThat(env).isNotNull();
        assertThat(env.getName()).isEqualTo("user-service");
        assertThat(env.getPropertySources()).isNotEmpty();
        Map<?, ?> source = env.getPropertySources().get(0).getSource();
        assertThat(source.get("minio.bucket.users")).isEqualTo("merchly-users");
    }

    @Test
    @DisplayName("для неизвестного сервиса возвращает пустой набор источников, а не ошибку")
    void unknownApplicationYieldsEmptySources() {
        Environment env = client.get()
                .uri("/no-such-service/default")
                .retrieve()
                .body(Environment.class);

        assertThat(env).isNotNull();
        assertThat(env.getPropertySources()).isEmpty();
    }

}
