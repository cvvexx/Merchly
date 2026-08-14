package io.cvvexxx.orders.exception;

public class OrderCannotConfirmException extends IllegalStateException implements LocalizedException {
    private static final String DEFAULT_CODE = "order.errors.cannot_confirm";
    private final Object[] args;

    public OrderCannotConfirmException(Object... args) {
        super(DEFAULT_CODE);
        this.args = args != null ? args : new Object[0];
    }

    @Override
    public Object[] getArgs() {
        return args;
    }
}