package io.cvvexxx.frontend.client.order;

import io.cvvexxx.frontend.dto.order.NewOrderDto;
import io.cvvexxx.frontend.dto.order.OrderDto;
import io.cvvexxx.frontend.dto.order.OrderStatusDto;
import io.cvvexxx.frontend.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class RestClientOrdersRestClient implements OrdersRestClient {

    private final RestClient restClient;

    @Override
    public OrderDto createOrder(NewOrderDto newOrderDto) {
        try {
            return restClient
                    .post()
                    .uri("/api/orders/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(newOrderDto)
                    .retrieve()
                    .body(OrderDto.class);
        } catch (HttpClientErrorException exception) {
            throw mapToCustomException(exception);
        }
    }

    @Override
    public OrderDto confirmOrder(UUID orderId) {
        try {
            return restClient
                    .post()
                    .uri("/api/orders/{orderId}/confirm", orderId)
                    .retrieve()
                    .body(OrderDto.class);
        } catch (HttpClientErrorException exception) {
            throw mapToCustomException(exception);
        }
    }

    @Override
    public OrderDto cancelOrder(UUID orderId) {
        try {
            return restClient
                    .post()
                    .uri("/api/orders/{orderId}/cancel", orderId)
                    .retrieve()
                    .body(OrderDto.class);
        } catch (HttpClientErrorException exception) {
            throw mapToCustomException(exception);
        }
    }

    @Override
    public OrderDto getOrder(UUID orderId) {
        try {
            return restClient
                    .get()
                    .uri("/api/orders/{orderId}", orderId)
                    .retrieve()
                    .body(OrderDto.class);
        } catch (HttpClientErrorException exception) {
            throw mapToCustomException(exception);
        }
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

    @Override
    public OrderStatusDto getOrderStatus(UUID orderId) {
        return restClient
                .get()
                .uri("/api/orders/{orderId}/status", orderId)
                .retrieve()
                .body(OrderStatusDto.class);
    }

    private List<String> extractBadRequestErrors(HttpClientErrorException exception) {
        ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
        if (problemDetail == null) {
            return List.of("Неизвестная ошибка сервера");
        }

        if (problemDetail.getProperties() != null && problemDetail.getProperties().containsKey("errors")) {
            Object errorsObj = problemDetail.getProperties().get("errors");
            if (errorsObj instanceof List<?> list) {
                return list.stream()
                        .map(Object::toString)
                        .toList();
            }
        }

        if (problemDetail.getDetail() != null) {
            return List.of(problemDetail.getDetail());
        }

        return List.of("Произошла ошибка при обработке запроса");
    }

    private BaseClientException mapToCustomException(HttpClientErrorException ex) {
        List<String> errors = extractBadRequestErrors(ex);

        return switch (ex.getStatusCode().value()) {
            case 400 -> new BadRequestException(errors);
            case 403 -> new OrderAccessDeniedException(errors);
            case 404 -> new OrderNotFoundException(errors);
            case 409 -> new OrderCannotChangeStatusException(errors);
            default -> new BaseClientException(errors) {
            };
        };
    }
}
