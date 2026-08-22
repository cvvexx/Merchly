package io.cvvexxx.orders.service;

import io.cvvexxx.orders.client.ProductsRestClient;
import io.cvvexxx.orders.client.UsersRestClient;
import io.cvvexxx.orders.domain.OrderStatus;
import io.cvvexxx.orders.dto.*;
import io.cvvexxx.orders.entity.Order;
import io.cvvexxx.orders.entity.OrderItem;
import io.cvvexxx.orders.event.OrderCancelledEvent;
import io.cvvexxx.orders.event.OrderCreatedEvent;
import io.cvvexxx.orders.event.OrderItemPayload;
import io.cvvexxx.orders.exception.*;
import io.cvvexxx.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultOrderService implements OrderService {

    private static final String DEFAULT_ORDER_NAME = "order";
    private static final String DEFAULT_USER_ORDER_NAME = "userOrder";

    private final OrderRepository orderRepository;
    private final ProductsRestClient productsRestClient;
    private final UsersRestClient usersRestClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = DEFAULT_USER_ORDER_NAME, key = "#currentUserId")
    public OrderDto createOrder(NewOrderDto newOrderDto, UUID currentUserId) {
        List<NewOrderItemDto> items = newOrderDto.items();

        Map<UUID, Integer> quantityByProduct = items.stream()
                .collect(Collectors.toMap(
                        NewOrderItemDto::productId,
                        NewOrderItemDto::quantity,
                        Integer::sum,
                        LinkedHashMap::new
                ));

        List<UUID> productIds = new ArrayList<>(quantityByProduct.keySet());

        List<ProductDto> fetchedProducts = productsRestClient.findAllProductsByIds(productIds);

        Map<UUID, ProductDto> productMap = fetchedProducts.stream()
                .collect(Collectors.toMap(ProductDto::id, Function.identity()));

        if (productMap.size() != productIds.size()) {
            throw new NoSuchElementException("order.errors.product.not_found");
        }

        Order order = Order.builder()
                .userId(currentUserId)
                .status(OrderStatus.PENDING)
                .deliveryAddress(newOrderDto.deliveryAddress())
                .comment(newOrderDto.comment())
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Map.Entry<UUID, Integer> entry : quantityByProduct.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();

            ProductDto product = productMap.get(productId);
            if (product == null) {
                throw new NoSuchElementException("order.errors.product.not_found");
            }

            BigDecimal price = product.price();
            if (price == null) {
                throw new IllegalStateException("order.errors.product.price_missing");
            }

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
        usersRestClient.clearUserCart();
        applicationEventPublisher.publishEvent(
                new OrderCreatedEvent(
                        savedOrder.getId(),
                        savedOrder.getTotalAmount(),
                        savedOrder.getItems()
                                .stream().map(orderItem -> new OrderItemPayload(
                                        orderItem.getProductId(),
                                        orderItem.getQuantity()
                                )).toList()
                )
        );
        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = DEFAULT_ORDER_NAME, key = "#orderId"),
            @CacheEvict(value = DEFAULT_USER_ORDER_NAME, key = "#result.userId()")
    })
    public OrderDto confirmOrder(UUID orderId, UUID currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            throw new OrderAccessDeniedException();
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderCannotConfirmException(orderId);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = DEFAULT_ORDER_NAME, key = "#orderId"),
            @CacheEvict(value = DEFAULT_USER_ORDER_NAME, key = "#result.userId()")
    })
    public OrderDto cancelOrderByUser(UUID orderId, UUID currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            throw new OrderAccessDeniedException();
        }

        if (OrderStatus.CONFIRMED.equals(order.getStatus()) || OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new OrderCannotCancelException(orderId);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        applicationEventPublisher.publishEvent(
                new OrderCancelledEvent(
                        savedOrder.getId(),
                        savedOrder.getItems()
                                .stream().map(orderItem -> new OrderItemPayload(
                                        orderItem.getProductId(),
                                        orderItem.getQuantity()
                                )).toList()
                )
        );

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = DEFAULT_ORDER_NAME, key = "#orderId")
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
    @Cacheable(value = DEFAULT_USER_ORDER_NAME, key = "#currentUserId")
    public List<OrderDto> getCurrentUserOrders(UUID currentUserId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserId)
                .orElseThrow(() -> new UserOrdersNotFoundException(currentUserId))
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = DEFAULT_ORDER_NAME, key = "#orderId"),
            @CacheEvict(value = DEFAULT_USER_ORDER_NAME, key = "#result.userId()")
    })
    public OrderDto cancelOrderBySystem(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Заказ {} уже отменен, повторная обработка OrderFailedEvent игнорируется", orderId);
            return mapToDto(order);
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.warn("Получен OrderFailedEvent для уже подтвержденного заказа {}, статус не изменен", orderId);
            return mapToDto(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);

        return mapToDto(order);
    }

    private OrderDto mapToDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getComment(),
                order.getCancellationReason(),
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