package io.cvvexxx.frontend.exception;

import java.util.List;

public class OrderCannotChangeStatusException extends RuntimeException {
    private final List<String> errors;

    public OrderCannotChangeStatusException(List<String> errors) {
        this.errors = errors;
    }

    public OrderCannotChangeStatusException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}
