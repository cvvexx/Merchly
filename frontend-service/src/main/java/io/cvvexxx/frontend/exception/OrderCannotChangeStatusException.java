package io.cvvexxx.frontend.exception;

import java.util.List;

public class OrderCannotChangeStatusException extends BaseClientException {
    public OrderCannotChangeStatusException(List<String> errors) {
        super(errors);
    }
}
