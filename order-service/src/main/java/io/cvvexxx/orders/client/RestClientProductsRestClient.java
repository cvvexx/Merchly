package io.cvvexxx.orders.client;

import io.cvvexxx.orders.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class RestClientProductsRestClient implements ProductsRestClient {

    private final RestClient restClient;

    @Override
    public List<ProductDto> findAllProductsByIds(List<UUID> ids) {
        return restClient
                .get()
                .uri("/api/internal/products?ids={ids}", StringUtils.collectionToCommaDelimitedString(ids))
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductDto>>() {
                });
    }

    @Override
    public ProductDto findById(UUID productId) {
        try {
            ProductDto product = restClient.get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .body(ProductDto.class);
            log.info("product found: {}", product);
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
