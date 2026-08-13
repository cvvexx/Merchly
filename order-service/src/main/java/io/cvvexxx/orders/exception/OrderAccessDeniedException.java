package io.cvvexxx.orders.exception;

import org.springframework.security.access.AccessDeniedException;

public class OrderAccessDeniedException extends AccessDeniedException implements LocalizedException {
    private static final String DEFAULT_CODE = "order.errors.access_denied";
    private final Object[] args;

    public OrderAccessDeniedException(Object... args) {
        super(DEFAULT_CODE);
        this.args = args != null ? args : new Object[0];
    }

    @Override
    public Object[] getArgs() {
        return args;
    }
}