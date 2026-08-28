package io.cvvexxx.frontend.service.order;

import io.cvvexxx.frontend.client.product.internal.ProductsInternalRestClient;
import io.cvvexxx.frontend.dto.order.OrderDto;
import io.cvvexxx.frontend.dto.order.OrderItemDto;
import io.cvvexxx.frontend.dto.order.OrderStatus;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.view.OrderDetailsView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOrderServiceTest {

    @Mock
    private ProductsInternalRestClient productsRestClient;

    @InjectMocks
    private DefaultOrderService orderService;

    @Test
    @DisplayName("enrichOrders: пустой/null список заказов не обращается к product-service и возвращает пустой список")
    void enrichOrders_WithEmptyOrNullOrders_ShouldReturnEmptyListWithoutCallingProductService() {
        var resultForNull = orderService.enrichOrders(null);
        var resultForEmpty = orderService.enrichOrders(List.of());

        assertEquals(List.of(), resultForNull);
        assertEquals(List.of(), resultForEmpty);
        verifyNoInteractions(productsRestClient);
    }

    @Test
    @DisplayName("enrichOrders: подставляет название товара из product-service и запрашивает только уникальные id")
    void enrichOrders_ShouldMapProductTitlesAndDeduplicateProductIds() {
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        OrderDto order = order(List.of(
                orderItem(productId, 2),
                orderItem(productId, 1),
                orderItem(otherProductId, 1)
        ));

        when(productsRestClient.findAllProductsByIds(List.of(productId, otherProductId)))
                .thenReturn(List.of(product(productId, "Футболка"), product(otherProductId, "Кружка")));

        List<OrderDetailsView> result = orderService.enrichOrders(List.of(order));

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).items().size());
        assertEquals("Футболка", result.get(0).items().get(0).title());
        assertEquals("Футболка", result.get(0).items().get(1).title());
        assertEquals("Кружка", result.get(0).items().get(2).title());

        ArgumentCaptor<List<UUID>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(productsRestClient).findAllProductsByIds(idsCaptor.capture());
        assertEquals(2, idsCaptor.getValue().size());
    }

    @Test
    @DisplayName("enrichOrders: если товар не найден в product-service, подставляет заглушку вместо падения")
    void enrichOrders_WhenProductMissingFromProductService_ShouldFallBackToPlaceholderTitle() {
        UUID productId = UUID.randomUUID();
        OrderDto order = order(List.of(orderItem(productId, 1)));
        when(productsRestClient.findAllProductsByIds(List.of(productId))).thenReturn(List.of());

        List<OrderDetailsView> result = orderService.enrichOrders(List.of(order));

        assertEquals("Товар не найден", result.get(0).items().get(0).title());
    }

    @Test
    @DisplayName("enrichOrder: null заказ -> null результат")
    void enrichOrder_WhenOrderIsNull_ShouldReturnNull() {
        OrderDetailsView result = orderService.enrichOrder(null);

        assertNull(result);
        verifyNoInteractions(productsRestClient);
    }

    @Test
    @DisplayName("enrichOrder: делегирует в enrichOrders и возвращает первый результат")
    void enrichOrder_ShouldReturnEnrichedOrder() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderDto order = order(orderId, List.of(orderItem(productId, 1)));

        when(productsRestClient.findAllProductsByIds(List.of(productId)))
                .thenReturn(List.of(product(productId, "Кружка")));

        OrderDetailsView result = orderService.enrichOrder(order);

        assertEquals(orderId, result.id());
        assertEquals("Кружка", result.items().get(0).title());
    }

    private OrderDto order(List<OrderItemDto> items) {
        return order(UUID.randomUUID(), items);
    }

    private OrderDto order(UUID orderId, List<OrderItemDto> items) {
        return new OrderDto(
                orderId, UUID.randomUUID(), OrderStatus.PENDING, new BigDecimal("100.00"),
                "address", "comment", null, items, Instant.now(), Instant.now()
        );
    }

    private OrderItemDto orderItem(UUID productId, int quantity) {
        return new OrderItemDto(UUID.randomUUID(), productId, new BigDecimal("50.00"), quantity);
    }

    private Product product(UUID productId, String title) {
        return new Product(productId, title, "desc", 10, new BigDecimal("50.00"), "image.png", UUID.randomUUID());
    }
}
