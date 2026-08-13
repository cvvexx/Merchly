package io.cvvexxx.orders.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class OrderNotFoundException extends RuntimeException {
    private final Object[] args;

    public OrderNotFoundException(UUID orderId) {
        super("order.errors.not_found");
        this.args = new Object[] { orderId };
    }
}
