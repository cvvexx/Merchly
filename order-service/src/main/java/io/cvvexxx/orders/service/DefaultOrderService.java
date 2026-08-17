package io.cvvexxx.orders.service;

import io.cvvexxx.orders.client.ProductsRestClient;
import io.cvvexxx.orders.domain.OrderStatus;
import io.cvvexxx.orders.dto.*;
import io.cvvexxx.orders.entity.Order;
import io.cvvexxx.orders.entity.OrderItem;
import io.cvvexxx.orders.event.OrderCreatedEvent;
import io.cvvexxx.orders.exception.OrderAccessDeniedException;
import io.cvvexxx.orders.exception.OrderCannotCancelException;
import io.cvvexxx.orders.exception.OrderCannotConfirmException;
import io.cvvexxx.orders.exception.OrderNotFoundException;
import io.cvvexxx.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultOrderService implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductsRestClient productClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public OrderDto createOrder(NewOrderDto newOrderDto, UUID currentUserId) {
        List<NewOrderItemDto> items = newOrderDto.items();

        Map<UUID, Integer> quantityByProduct = items.stream()
                .collect(Collectors.toMap(
                        NewOrderItemDto::productId,
                        NewOrderItemDto::quantity,
                        Integer::sum,
                        LinkedHashMap::new
                ));
        Order order = Order.builder()
                .userId(currentUserId)
                .status(OrderStatus.CREATED)
                .deliveryAddress(newOrderDto.deliveryAddress())
                .comment(newOrderDto.comment())
                .build();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Map.Entry<UUID, Integer> entry : quantityByProduct.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();
            ProductDto product = productClient.findById(productId);
            BigDecimal price = product.price();
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(quantity));
            totalPrice = totalPrice.add(itemTotal);
            order.addItem(new OrderItem(
                    null,
                    order,
                    productId,
                    price,
                    quantity
            ));
        }
        order.setTotalAmount(totalPrice);
        Order savedOrder = orderRepository.save(order);
        applicationEventPublisher.publishEvent(
                new OrderCreatedEvent(savedOrder.getId(), savedOrder.getTotalAmount())
        );
        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto confirmOrder(UUID orderId, UUID currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            throw new OrderAccessDeniedException();
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new OrderCannotConfirmException(orderId);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(UUID orderId, UUID currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            throw new OrderAccessDeniedException();
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new OrderCannotCancelException(orderId);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, UUID currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            throw new OrderAccessDeniedException();
        }

        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getCurrentUserOrders(UUID currentUserId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(this::mapToDto)
                .toList();
    }

    private OrderDto mapToDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getComment(),
                order.getItems().stream()
                        .map(orderItem -> new OrderItemDto(
                                orderItem.getId(),
                                orderItem.getProductId(),
                                orderItem.getPrice(),
                                orderItem.getQuantity()
                        ))
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}