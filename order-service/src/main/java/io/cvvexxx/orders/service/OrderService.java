package io.cvvexxx.orders.service;

import io.cvvexxx.orders.domain.OrderStatus;
import io.cvvexxx.orders.dto.NewOrderDto;
import io.cvvexxx.orders.dto.OrderDto;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderDto createOrder(NewOrderDto newOrderDto, UUID currentUserId);

    OrderDto confirmOrder(UUID orderId, UUID currentUserId, boolean isAdmin);

    OrderDto cancelOrderByUser(UUID orderId, UUID currentUserId, boolean isAdmin);

    OrderDto cancelOrderBySystem(UUID orderId, String reason);

    OrderDto getOrder(UUID orderId, UUID currentUserId, boolean isAdmin);

    List<OrderDto> getCurrentUserOrders(UUID currentUserId);
}
