package io.cvvexxx.orders.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;


@RequiredArgsConstructor
@Slf4j
public class RestClientUsersRestClient implements UsersRestClient {

    private final RestClient restClient;

    @Override
    public void clearUserCart() {
        log.info("Clearing user cart...");
        restClient//TODO(ADD try/catch)
                .delete()
                .uri("/api/users/cart")
                .retrieve()
                .toBodilessEntity();
    }
}
