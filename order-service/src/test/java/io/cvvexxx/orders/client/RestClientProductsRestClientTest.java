package io.cvvexxx.orders.client;

import io.cvvexxx.orders.dto.ProductDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestClientProductsRestClientTest {

    private MockRestServiceServer mockServer;
    private RestClientProductsRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://product-service");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientProductsRestClient(builder.build());
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    @DisplayName("findById: возвращает товар при успешном ответе от product-service")
    void findById_WhenProductExists_ShouldReturnProduct() {
        UUID productId = UUID.randomUUID();
        mockServer.expect(requestTo("http://product-service/api/products/" + productId))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":\"" + productId + "\",\"quantity\":5,\"price\":10.00}",
                        MediaType.APPLICATION_JSON
                ));

        ProductDto result = client.findById(productId);

        assertEquals(productId, result.id());
    }

    @Test
    @DisplayName("findById: при 404 от product-service выбрасывает NoSuchElementException")
    void findById_WhenProductNotFound_ShouldThrowNoSuchElementException() {
        UUID productId = UUID.randomUUID();
        mockServer.expect(requestTo("http://product-service/api/products/" + productId))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(NoSuchElementException.class, () -> client.findById(productId));
    }

    @Test
    @DisplayName("findById: при ошибке сервера выбрасывает IllegalStateException")
    void findById_WhenServerError_ShouldThrowIllegalStateException() {
        UUID productId = UUID.randomUUID();
        mockServer.expect(requestTo("http://product-service/api/products/" + productId))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(IllegalStateException.class, () -> client.findById(productId));
    }
}
