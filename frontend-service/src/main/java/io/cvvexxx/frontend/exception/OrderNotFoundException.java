package io.cvvexxx.frontend.exception;

import java.util.List;

public class OrderNotFoundException extends RuntimeException {
    List<String> errors;

    public OrderNotFoundException(List<String> errors) {
        super();
        this.errors = errors;
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}
