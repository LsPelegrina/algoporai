package com.rinha.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PaymentSummary(
        @JsonProperty("default") SummaryData Default,
        @JsonProperty("fallback") SummaryData Fallback
) {
        public record SummaryData(
                long totalRequests,
                BigDecimal totalAmount
        ) {}
}