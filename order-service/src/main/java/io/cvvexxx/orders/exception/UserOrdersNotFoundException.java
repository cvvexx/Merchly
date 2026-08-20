package io.cvvexxx.orders.exception;

public class UserOrdersNotFoundException extends IllegalStateException implements LocalizedException {
    private static final String DEFAULT_CODE = "order.errors.user_orders_not_found";
    private final Object[] args;

    public UserOrdersNotFoundException(Object... args) {
        super(DEFAULT_CODE);
        this.args = args != null ? args : new Object[0];
    }

    @Override
    public Object[] getArgs() {
        return args;
    }
}
