package io.cvvexxx.orders.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, BigDecimal totalAmount) {
}