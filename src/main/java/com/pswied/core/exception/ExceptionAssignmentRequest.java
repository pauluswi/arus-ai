package com.pswied.core.exception;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExceptionAssignmentRequest(
        @NotBlank @Size(max = 120) String assignedTo) {
}
