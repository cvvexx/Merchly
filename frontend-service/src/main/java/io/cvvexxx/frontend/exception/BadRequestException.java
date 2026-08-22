package io.cvvexxx.frontend.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
public class BadRequestException extends BaseClientException {
    public BadRequestException(List<String> errors) {
        super(errors);
    }
}
