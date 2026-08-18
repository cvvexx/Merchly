package io.cvvexxx.frontend.controller.order;

import io.cvvexxx.frontend.client.order.OrdersRestClient;
import io.cvvexxx.frontend.client.order.RestClientOrdersRestClient;
import io.cvvexxx.frontend.dto.order.NewOrderDto;
import io.cvvexxx.frontend.dto.order.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/orders")
public class OrderController {

    private final OrdersRestClient restClient;

    @GetMapping
    public String getUserOrders(Model model) {
        List<OrderDto> orders = restClient.getUserOrders();
        model.addAttribute("orders", orders);
        return "order/user-orders";
    }

    @GetMapping("/{orderId}")
    public String getOrderPage(Model model, @PathVariable UUID orderId) {
        OrderDto order = restClient.getOrder(orderId);
        model.addAttribute("order", order);
        return "order/order";
    }

    @PostMapping("/create")
    public String createOrder(NewOrderDto newOrderDto) {
        OrderDto order = restClient.createOrder(newOrderDto);
        return "redirect:/orders/" + order.id();
    }

    @PostMapping("/{orderId}/confirm")
    public String confirmOrder(Model model, @PathVariable UUID orderId) {
        OrderDto order = restClient.confirmOrder(orderId);
        model.addAttribute("order", order);
        return "order/order";
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(Model model, @PathVariable UUID orderId) {
        OrderDto order = restClient.cancelOrder(orderId);
        model.addAttribute("order", order);
        return "order/order";
    }

}
