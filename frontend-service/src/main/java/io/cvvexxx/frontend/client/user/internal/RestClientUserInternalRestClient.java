package io.cvvexxx.frontend.client.user.internal;

import io.cvvexxx.frontend.dto.ProductOwnerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;


@RequiredArgsConstructor
public class RestClientUserInternalRestClient implements UserInternalRestClient {

    private final RestClient restClient;

    @Override
    public List<ProductOwnerDto> findAllUsersByIds(List<Integer> userIds) {
        return restClient
                .get()
                .uri("/api/internal/users?ids={ids}", StringUtils.collectionToCommaDelimitedString(userIds))
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductOwnerDto>>() {
                });
    }

    @Override
    public ProductOwnerDto findUserById(Integer userId) {
        return restClient
                .get()
                .uri("/api/internal/users/{id}", userId)
                .retrieve()
                .body(ProductOwnerDto.class);
    }
}
