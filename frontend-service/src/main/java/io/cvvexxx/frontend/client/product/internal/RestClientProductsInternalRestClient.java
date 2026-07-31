package io.cvvexxx.frontend.client.product.internal;

import io.cvvexxx.frontend.dto.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
public class RestClientProductsInternalRestClient implements ProductsInternalRestClient {

    private final RestClient restClient;

    @Override
    public List<Product> findAllProductsByIds(List<UUID> ids) {
        return restClient
                .get()
                .uri("/api/internal/products?ids={ids}", StringUtils.collectionToCommaDelimitedString(ids))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Product>>() {
                });
    }
}
