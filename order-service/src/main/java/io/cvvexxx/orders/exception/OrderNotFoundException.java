package io.cvvexxx.orders.exception;

import lombok.Getter;

@Getter
public class OrderNotFoundException extends RuntimeException implements LocalizedException {
    private static final String DEFAULT_CODE = "order.errors.order_not_found";
    private final Object[] args;

    public OrderNotFoundException(Object... args) {
        super(DEFAULT_CODE);
        this.args = args != null ? args : new Object[0];
    }

    @Override
    public Object[] getArgs() {
        return args;
    }
}
