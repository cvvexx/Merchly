package io.cvvexxx.frontend.client.order;

import io.cvvexxx.frontend.dto.order.NewOrderDto;
import io.cvvexxx.frontend.dto.order.OrderDto;

import java.util.List;
import java.util.UUID;

public interface OrdersRestClient {

    OrderDto createOrder(NewOrderDto newOrderDto);

    OrderDto confirmOrder(UUID orderId);

    OrderDto cancelOrder(UUID orderId);

    OrderDto getOrder(UUID orderId);

    List<OrderDto> getUserOrders();
}
