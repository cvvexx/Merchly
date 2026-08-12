package io.cvvexxx.orders.client;

import io.cvvexxx.orders.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.NoSuchElementException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestClientProductClient implements ProductClient {

    private final RestClient productServiceRestClient;

    @Override
    public ProductDto findById(UUID productId) {
        try {
            ProductDto product = productServiceRestClient.get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .body(ProductDto.class);

            if (product == null) {
                throw new NoSuchElementException("order.errors.product.not_found");
            }

            return product;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NoSuchElementException("order.errors.product.not_found");
            }

            log.error("Failed to fetch product {} from product-service: {}", productId, exception.getMessage());
            throw new IllegalStateException("order.errors.product.unavailable", exception);
        }
    }
}
