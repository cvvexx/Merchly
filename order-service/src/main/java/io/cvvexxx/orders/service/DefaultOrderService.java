package io.cvvexxx.orders.service;

import io.cvvexxx.orders.client.RestClientProductsRestClient;
import io.cvvexxx.orders.domain.OrderStatus;
import io.cvvexxx.orders.dto.*;
import io.cvvexxx.orders.entity.Order;
import io.cvvexxx.orders.entity.OrderItem;
import io.cvvexxx.orders.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultOrderService implements OrderService {

    private final OrderRepository orderRepository;
    private final RestClientProductsRestClient restClient;

    @Override
    @Transactional
    public OrderDto createOrder(NewOrderDto newOrderDto, UUID currentUserId) {
        List<NewOrderItemDto> items = newOrderDto.items();

        List<ProductDto> products = restClient.findAllProductsByIds(
                items.stream().map(NewOrderItemDto::productId).toList());

        Map<UUID, ProductDto> productById = products.stream()
                .collect(Collectors.toMap(ProductDto::id, Function.identity()));

        Order order = Order.builder()
                .userId(currentUserId)
                .status(OrderStatus.CREATED)
                .deliveryAddress(newOrderDto.deliveryAddress())
                .comment(newOrderDto.comment())
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (NewOrderItemDto item : items) {
            ProductDto product = productById.get(item.productId());
            if (product == null) {
                throw new EntityNotFoundException("Product %s not found".formatted(item.productId()));
            }
            BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(item.quantity()));
            totalPrice = totalPrice.add(itemTotal);

            order.addItem(new OrderItem(
                    UUID.randomUUID(),
                    order,
                    product.id(),
                    product.price(),
                    item.quantity()
            ));
        }
        order.setTotalAmount(totalPrice);

        Order savedOrder = orderRepository.save(order);

        //TODO(KAFKA)

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto confirmOrder(UUID orderId, UUID currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id %s not found".formatted(orderId)));

        if (!isAdmin || !order.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Вам нельзя изменять этот товар");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Заказ уже отменен, его нельзя подтвердить");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        //TODO(KAFKA)

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(UUID orderId, UUID currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id %s not found".formatted(orderId)));

        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Вам нельзя изменять этот товар");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Заказ уже успешно отменен");
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Заказ уже успешно подтвержден, его нельзя отменить");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        //TODO(KAFKA)

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, UUID currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id %s not found".formatted(orderId)));

        if (!order.getUserId().equals(currentUserId) || !isAdmin) {
            throw new AccessDeniedException("Вам нельзя изменять этот товар");
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
