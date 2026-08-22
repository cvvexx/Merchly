package io.cvvexxx.orders.exception;

public class OrderCannotCancelException extends IllegalStateException implements LocalizedException {
    private static final String DEFAULT_CODE = "order.errors.cannot_cancel";
    private final Object[] args;

    public OrderCannotCancelException(Object... args) {
        super(DEFAULT_CODE);
        this.args = args != null ? args : new Object[0];
    }

    @Override
    public Object[] getArgs() {
        return args;
    }
}
