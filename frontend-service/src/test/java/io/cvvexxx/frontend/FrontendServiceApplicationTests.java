package io.cvvexxx.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@ActiveProfiles("test")
class FrontendServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
