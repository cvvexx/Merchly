package io.cvvexxx.orders.controller;

import io.cvvexxx.orders.dto.NewOrderDto;
import io.cvvexxx.orders.dto.OrderDto;
import io.cvvexxx.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody NewOrderDto newOrderDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal Jwt jwt,
            UriComponentsBuilder uriComponentsBuilder
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        }
        log.info("Received request to create order, dto: {}", newOrderDto);
        UUID currentUserId = currentUserId(jwt);
        log.info("Request received to create order for user {}", currentUserId);
        OrderDto createdOrder = orderService.createOrder(newOrderDto, currentUserId);

        return ResponseEntity.created(
                        uriComponentsBuilder
                                .replacePath("/api/orders/{orderId}")
                                .build(Map.of("orderId", createdOrder.id()))
                )
                .body(createdOrder);
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderDto> confirmOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID currentUserId = currentUserId(jwt);
        log.info("Request received to confirm order {} by user {}", orderId, currentUserId);
        return ResponseEntity.ok(orderService.confirmOrder(orderId, currentUserId, hasAdminRole(jwt)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID currentUserId = currentUserId(jwt);
        log.info("Request received to cancel order {} by user {}", orderId, currentUserId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, currentUserId, hasAdminRole(jwt)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(orderService.getOrder(orderId, currentUserId(jwt), hasAdminRole(jwt)));
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getCurrentUserOrders(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.getCurrentUserOrders(currentUserId(jwt)));
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("sub"));
    }

    private boolean hasAdminRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            return roles.contains("ADMIN") || roles.contains("ROLE_ADMIN");
        }
        return false;
    }
}
