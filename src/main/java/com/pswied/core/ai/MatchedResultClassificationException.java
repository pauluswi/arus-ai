package com.pswied.core.ai;

import java.util.UUID;

public class MatchedResultClassificationException extends RuntimeException {

    public MatchedResultClassificationException(UUID resultId) {
        super("Matched reconciliation result %s cannot be classified as an exception.".formatted(resultId));
    }
}
