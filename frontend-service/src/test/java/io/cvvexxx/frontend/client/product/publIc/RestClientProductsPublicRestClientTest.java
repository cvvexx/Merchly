package io.cvvexxx.frontend.client.product.publIc;

import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RestClientProductsPublicRestClientTest {

    private MockRestServiceServer server;
    private RestClientProductsPublicRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientProductsPublicRestClient(builder.build());
    }

    @Nested
    @DisplayName("findProductById")
    class FindProductByIdTests {

        @Test
        @DisplayName("если продукт не найден (404), возвращает Optional.empty() вместо исключения")
        void findProductById_When404_ShouldReturnEmptyOptional() {
            UUID productId = UUID.randomUUID();
            server.expect(requestTo("http://localhost/api/products/" + productId))
                    .andRespond(withStatus(NOT_FOUND));

            Optional<Product> result = client.findProductById(productId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProductTests {

        @Test
        @DisplayName("при 400 выбрасывает BadRequestException с ошибками из тела ответа")
        void createProduct_When400_ShouldThrowBadRequestExceptionWithErrors() {
            server.expect(requestTo("http://localhost/api/products"))
                    .andRespond(withStatus(BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"title must not be blank\"]}"));
            var image = new MockMultipartFile("image", "image.png", "image/png", "123".getBytes());

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    client.createProduct("", "desc", 1, BigDecimal.TEN, image, UUID.randomUUID())
            );

            assertEquals(List.of("title must not be blank"), exception.getErrors());
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProductTests {

        @Test
        @DisplayName("при 400 выбрасывает BadRequestException с ошибками из тела ответа")
        void updateProduct_When400_ShouldThrowBadRequestExceptionWithErrors() {
            UUID productId = UUID.randomUUID();
            server.expect(requestTo("http://localhost/api/products/" + productId))
                    .andRespond(withStatus(BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"price must be positive\"]}"));
            var image = new MockMultipartFile("image", "image.png", "image/png", "123".getBytes());

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    client.updateProduct(productId, "title", "desc", 1, BigDecimal.ZERO, image)
            );

            assertEquals(List.of("price must be positive"), exception.getErrors());
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProductTests {

        @Test
        @DisplayName("при 404 выбрасывает NoSuchElementException")
        void deleteProduct_When404_ShouldThrowNoSuchElementException() {
            UUID productId = UUID.randomUUID();
            server.expect(requestTo("http://localhost/api/products/" + productId))
                    .andRespond(withStatus(NOT_FOUND));

            assertThrows(NoSuchElementException.class, () -> client.deleteProduct(productId));
        }
    }
}
