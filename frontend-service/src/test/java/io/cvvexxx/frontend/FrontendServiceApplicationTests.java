package io.cvvexxx.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
class FrontendServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class RedisTestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

        @Bean
        ConfigureRedisAction configureRedisAction() {
            return ConfigureRedisAction.NO_OP;
        }
    }
}
