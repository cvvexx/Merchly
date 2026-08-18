package io.cvvexxx.frontend.service.order;

import io.cvvexxx.frontend.client.product.internal.ProductsInternalRestClient;
import io.cvvexxx.frontend.dto.order.OrderDto;
import io.cvvexxx.frontend.dto.order.OrderItemDto;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.view.OrderDetailsView;
import io.cvvexxx.frontend.view.OrderItemDetailsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultOrderService implements  OrderService {

    private final ProductsInternalRestClient productsRestClient;

    @Override
    public List<OrderDetailsView> enrichOrders(List<OrderDto> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> productIds = orders.stream()
                .flatMap(order -> order.items().stream())
                .map(OrderItemDto::productId)
                .distinct()
                .toList();

        List<Product> products = productsRestClient.findAllProductsByIds(productIds);

        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::id, Function.identity(), (p1, p2) -> p1));

        return orders.stream()
                .map(order -> mapToDetailsView(order, productMap))
                .toList();
    }

    @Override
    public OrderDetailsView enrichOrder(OrderDto order) {
        if (order == null) {
            return null;
        }
        return enrichOrders(List.of(order)).stream().findFirst().orElse(null);
    }

    private OrderDetailsView mapToDetailsView(OrderDto order, Map<UUID, Product> productMap) {
        List<OrderItemDetailsView> items = order.items().stream()
                .map(item -> {
                    Product product = productMap.get(item.productId());
                    String productName = (product != null) ? product.title() : "Товар не найден";
                    return new OrderItemDetailsView(
                            item.id(),
                            item.productId(),
                            productName,
                            item.price(),
                            item.quantity()
                    );
                })
                .toList();

        return new OrderDetailsView(
                order.id(),
                order.userId(),
                order.status(),
                order.totalAmount(),
                order.deliveryAddress(),
                order.comment(),
                items,
                order.createdAt(),
                order.updatedAt()
        );
    }
}