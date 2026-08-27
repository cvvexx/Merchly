package io.cvvexxx.frontend.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real end-to-end test: a live embedded servlet context, the actual Spring Security
 * filter chain, a real Redis-backed HTTP session (Testcontainers), and real outbound
 * RestClient HTTP calls against WireMock standing in for product/user/reviews services.
 * No address (Redis host/port, downstream base URLs) is hardcoded - everything is
 * discovered at runtime via @DynamicPropertySource, exactly like production discovers
 * its own addresses through environment variables per docker-compose.prod.yml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductsListFlowIT {

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        // api_gateway задан в application-test.properties реальным адресом шлюза (8086),
        // поэтому его обязательно перекрывать - иначе тест уйдёт в живой шлюз.
        registry.add("spring.restclient.uri.api_gateway", wireMock::baseUrl);
        registry.add("spring.restclient.uri.product_service", wireMock::baseUrl);
        registry.add("spring.restclient.uri.user_service", wireMock::baseUrl);
        registry.add("spring.restclient.uri.reviews_service", wireMock::baseUrl);
        registry.add("spring.security.oauth2.client.provider.keycloak.token-uri", () -> wireMock.baseUrl() + "/token");
    }

    @AfterEach
    void resetStubs() {
        wireMock.resetAll();
    }

    @Test
    void getProductsList_WhenProductsExist_ShouldRenderListWithOwnerAndReviewStatsFromRealHttpCalls() throws Exception {
        // given
        UUID productId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String productJson = """
                [{"id":"%s","title":"Wireless Mouse","description":"A mouse","quantity":5,"price":19.99,"imageFileName":"mouse.png","createdBy":"%s"}]
                """.formatted(productId, ownerId);
        String ownersJson = """
                [{"id":"%s","username":"seller1","avatarFileName":"avatar.png"}]
                """.formatted(ownerId);
        String statsJson = """
                [{"productId":"%s","averageRating":4.5,"totalReviews":10}]
                """.formatted(productId);

        wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/products"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(productJson)));
        wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/internal/users"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(ownersJson)));
        wireMock.stubFor(WireMock.post(urlPathEqualTo("/api/reviews/products/stats"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(statsJson)));
        stubClientCredentialsToken();

        var token = fakeUserToken();

        // when
        var result = mockMvc.perform(get("/catalogue/products/list")
                .param("filter", "mouse")
                .with(authentication(token)));

        // then
        result.andExpect(status().isOk())
                .andExpect(view().name("catalogue/products/list"))
                .andExpect(model().attributeExists("products"));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/products")));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/internal/users")));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/reviews/products/stats")));
    }

    @Test
    void getProductsList_WhenNoProductsReturned_ShouldRenderEmptyListWithoutCallingOwnerOrReviewServices() throws Exception {
        // given
        wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/products"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("[]")));

        var token = fakeUserToken();

        // when
        var result = mockMvc.perform(get("/catalogue/products/list")
                .param("filter", "nonexistent")
                .with(authentication(token)));

        // then
        result.andExpect(status().isOk())
                .andExpect(view().name("catalogue/products/list"));

        assertEquals(List.of(), result.andReturn().getModelAndView().getModel().get("products"));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/products")));
        assertThat(wireMock.findAllUnmatchedRequests()).isEmpty();
        assertEquals(0, wireMock.countRequestsMatching(postRequestedFor(urlPathEqualTo("/api/reviews/products/stats")).build()).getCount());
    }

    private void stubClientCredentialsToken() {
        wireMock.stubFor(WireMock.post(urlPathEqualTo("/token"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"service-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));
    }

    private KeycloakJwtAuthenticationToken fakeUserToken() {
        UUID userId = UUID.randomUUID();
        List<GrantedAuthority> authorities = Stream.of("ROLE_USER")
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        String notExpiredAccessToken = fakeJwt(userId, Instant.now().plus(1, ChronoUnit.HOURS));
        return new KeycloakJwtAuthenticationToken(userId.toString(), userId, notExpiredAccessToken, authorities);
    }

    private String fakeJwt(UUID subject, Instant expiresAt) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + subject + "\",\"exp\":" + expiresAt.getEpochSecond() + "}")
                        .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }
}
