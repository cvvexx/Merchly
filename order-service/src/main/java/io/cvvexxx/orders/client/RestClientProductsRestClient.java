package io.cvvexxx.orders.client;

import io.cvvexxx.orders.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
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

}
