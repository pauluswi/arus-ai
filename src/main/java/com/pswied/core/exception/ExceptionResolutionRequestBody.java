package com.pswied.core.exception;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExceptionResolutionRequestBody(
        @NotNull ExceptionResolutionAction action,
        @NotBlank @Size(max = 1000) String justification) {
}
