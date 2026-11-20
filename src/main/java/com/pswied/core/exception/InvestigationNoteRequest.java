package com.pswied.core.exception;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvestigationNoteRequest(
        @NotBlank @Size(max = 4000) String note) {
}
