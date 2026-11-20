package com.pswied.core.exception;

import jakarta.validation.constraints.NotNull;

public record ExceptionSeverityRequest(
        @NotNull ExceptionSeverity severity) {
}
