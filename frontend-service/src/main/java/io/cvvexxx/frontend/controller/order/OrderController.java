package io.cvvexxx.frontend.controller.order;

import io.cvvexxx.frontend.client.order.OrdersRestClient;
import io.cvvexxx.frontend.dto.order.NewOrderDto;
import io.cvvexxx.frontend.dto.order.OrderDto;
import io.cvvexxx.frontend.dto.order.OrderStatusDto;
import io.cvvexxx.frontend.service.order.DefaultOrderService;
import io.cvvexxx.frontend.view.OrderDetailsView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/orders")
public class OrderController {

    private final OrdersRestClient ordersRestClient;
    private final DefaultOrderService orderDetailsService;

    @GetMapping
    public String getUserOrders(Model model) {
        List<OrderDto> orders = ordersRestClient.getUserOrders();
        List<OrderDetailsView> enrichedOrders = orderDetailsService.enrichOrders(orders);
        model.addAttribute("orders", enrichedOrders);
        return "order/user-orders";
    }

    @GetMapping("/{orderId}")
    public String getOrderPage(Model model, @PathVariable UUID orderId) {
        OrderDto order = ordersRestClient.getOrder(orderId);
        OrderDetailsView enrichedOrder = orderDetailsService.enrichOrder(order);
        model.addAttribute("order", enrichedOrder);
        return "order/order";
    }

    @PostMapping("/create")
    public String createOrder(NewOrderDto newOrderDto) {
        OrderDto order = ordersRestClient.createOrder(newOrderDto);
        log.info("Order created: {}", order);
        return "redirect:/orders/" + order.id();
    }

    @PostMapping("/{orderId}/confirm")
    public String confirmOrder(@PathVariable UUID orderId) {
        ordersRestClient.confirmOrder(orderId);
        return "redirect:/orders/" + orderId;
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable UUID orderId) {
        ordersRestClient.cancelOrder(orderId);
        return "redirect:/orders/" + orderId;
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<Map<String, String>> getOrderStatusJson(@PathVariable UUID orderId) {
        OrderDto order = ordersRestClient.getOrder(orderId);

        String comment = order.cancellationReason() != null ? order.cancellationReason() : "";
        log.info("Order status: {}", order.status());
        log.info("Order comment: {}", comment);
        return ResponseEntity.ok(Map.of(
                "status", order.status().toString(),
                "comment", comment
        ));
    }
}