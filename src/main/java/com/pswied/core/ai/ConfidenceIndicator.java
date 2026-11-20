package com.pswied.core.ai;

import java.math.BigDecimal;

public record ConfidenceIndicator(
        BigDecimal score,
        String label) {
}
