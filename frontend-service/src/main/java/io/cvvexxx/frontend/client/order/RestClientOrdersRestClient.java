package io.cvvexxx.frontend.client.order;

import io.cvvexxx.frontend.dto.order.NewOrderDto;
import io.cvvexxx.frontend.dto.order.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class RestClientOrdersRestClient implements OrdersRestClient {

    private final RestClient restClient;

    //TODO(сделать обработку ошибок везде)
    @Override
    public OrderDto createOrder(NewOrderDto newOrderDto) {
        return restClient
                .post()
                .uri("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newOrderDto)
                .retrieve()
                .body(OrderDto.class);

    }

    @Override
    public OrderDto confirmOrder(UUID orderId) {
        return restClient
                .post()
                .uri("/api/orders/{orderId}/confirm", orderId)
                .retrieve()
                .body(OrderDto.class);
    }

    @Override
    public OrderDto cancelOrder(UUID orderId) {
        return restClient
                .post()
                .uri("/api/orders/{orderId}/cancel", orderId)
                .retrieve()
                .body(OrderDto.class);
    }

    @Override
    public OrderDto getOrder(UUID orderId) {
        return restClient
                .get()
                .uri("/api/orders/{orderId}", orderId)
                .retrieve()
                .body(OrderDto.class);
    }

    @Override
    public List<OrderDto> getUserOrders() {
        return restClient
                .get()
                .uri("/api/orders/")
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderDto>>() {
                });
    }
}
