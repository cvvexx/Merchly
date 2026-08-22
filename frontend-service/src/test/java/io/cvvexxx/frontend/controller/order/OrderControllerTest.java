package io.cvvexxx.frontend.controller.order;

import io.cvvexxx.frontend.client.order.OrdersRestClient;
import io.cvvexxx.frontend.dto.order.NewOrderDto;
import io.cvvexxx.frontend.dto.order.NewOrderItemDto;
import io.cvvexxx.frontend.dto.order.OrderDto;
import io.cvvexxx.frontend.dto.order.OrderStatus;
import io.cvvexxx.frontend.service.order.DefaultOrderService;
import io.cvvexxx.frontend.view.OrderDetailsView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrdersRestClient ordersRestClient;

    @Mock
    private DefaultOrderService orderDetailsService;

    @InjectMocks
    private OrderController controller;

    @Test
    @DisplayName("getUserOrders: получает заказы пользователя, обогащает их и наполняет модель")
    void getUserOrders_ShouldEnrichOrdersAndPopulateModel() {
        // given
        OrderDto order = order(UUID.randomUUID());
        List<OrderDto> orders = List.of(order);
        List<OrderDetailsView> enrichedOrders = List.of(enrichedOrder(order.id()));
        when(ordersRestClient.getUserOrders()).thenReturn(orders);
        when(orderDetailsService.enrichOrders(orders)).thenReturn(enrichedOrders);
        var model = new ConcurrentModel();

        // when
        String result = controller.getUserOrders(model);

        // then
        assertEquals("order/user-orders", result);
        assertEquals(enrichedOrders, model.getAttribute("orders"));
    }

    @Test
    @DisplayName("getOrderPage: получает заказ по id, обогащает его и наполняет модель")
    void getOrderPage_ShouldEnrichOrderAndPopulateModel() {
        // given
        UUID orderId = UUID.randomUUID();
        OrderDto order = order(orderId);
        OrderDetailsView enrichedOrder = enrichedOrder(orderId);
        when(ordersRestClient.getOrder(orderId)).thenReturn(order);
        when(orderDetailsService.enrichOrder(order)).thenReturn(enrichedOrder);
        var model = new ConcurrentModel();

        // when
        String result = controller.getOrderPage(model, orderId);

        // then
        assertEquals("order/order", result);
        assertEquals(enrichedOrder, model.getAttribute("order"));
    }

    @Test
    @DisplayName("createOrder: создаёт заказ и делает редирект на страницу созданного заказа")
    void createOrder_ShouldCreateOrderAndRedirectToItsPage() {
        // given
        UUID orderId = UUID.randomUUID();
        NewOrderDto newOrderDto = new NewOrderDto(List.of(new NewOrderItemDto(UUID.randomUUID(), 1)), "address", "comment");
        when(ordersRestClient.createOrder(newOrderDto)).thenReturn(order(orderId));

        // when
        String result = controller.createOrder(newOrderDto);

        // then
        assertEquals("redirect:/orders/" + orderId, result);
    }

    @Test
    @DisplayName("confirmOrder: подтверждает заказ и делает редирект на его страницу")
    void confirmOrder_ShouldConfirmAndRedirectToOrderPage() {
        // given
        UUID orderId = UUID.randomUUID();

        // when
        String result = controller.confirmOrder(orderId);

        // then
        assertEquals("redirect:/orders/" + orderId, result);
        verify(ordersRestClient).confirmOrder(orderId);
    }

    @Test
    @DisplayName("cancelOrder: отменяет заказ и делает редирект на его страницу")
    void cancelOrder_ShouldCancelAndRedirectToOrderPage() {
        // given
        UUID orderId = UUID.randomUUID();

        // when
        String result = controller.cancelOrder(orderId);

        // then
        assertEquals("redirect:/orders/" + orderId, result);
        verify(ordersRestClient).cancelOrder(orderId);
    }

    @Test
    @DisplayName("getOrderStatusJson: возвращает статус и причину отмены заказа")
    void getOrderStatusJson_WhenOrderCancelled_ShouldReturnStatusAndReason() {
        // given
        UUID orderId = UUID.randomUUID();
        OrderDto order = new OrderDto(
                orderId, UUID.randomUUID(), OrderStatus.CANCELLED, new BigDecimal("100.00"),
                "address", "comment", "out of stock", List.of(), Instant.now(), Instant.now()
        );
        when(ordersRestClient.getOrder(orderId)).thenReturn(order);

        // when
        ResponseEntity<Map<String, String>> response = controller.getOrderStatusJson(orderId);

        // then
        assertEquals(Map.of("status", "CANCELLED", "comment", "out of stock"), response.getBody());
    }

    @Test
    @DisplayName("getOrderStatusJson: если причины отмены нет, возвращает пустую строку в comment")
    void getOrderStatusJson_WhenNoCancellationReason_ShouldReturnEmptyComment() {
        // given
        UUID orderId = UUID.randomUUID();
        OrderDto order = order(orderId);
        when(ordersRestClient.getOrder(orderId)).thenReturn(order);

        // when
        ResponseEntity<Map<String, String>> response = controller.getOrderStatusJson(orderId);

        // then
        assertEquals(Map.of("status", "PENDING", "comment", ""), response.getBody());
    }

    private OrderDto order(UUID orderId) {
        return new OrderDto(
                orderId, UUID.randomUUID(), OrderStatus.PENDING, new BigDecimal("100.00"),
                "address", "comment", null, List.of(), Instant.now(), Instant.now()
        );
    }

    private OrderDetailsView enrichedOrder(UUID orderId) {
        return new OrderDetailsView(
                orderId, UUID.randomUUID(), OrderStatus.PENDING, new BigDecimal("100.00"),
                "address", "comment", null, List.of(), Instant.now(), Instant.now()
        );
    }
}
