package com.pswied.core.exception;

import java.util.UUID;

public class ExceptionResolutionRequestNotFoundException extends RuntimeException {

    public ExceptionResolutionRequestNotFoundException(UUID requestId) {
        super("Exception resolution request %s was not found.".formatted(requestId));
    }
}
