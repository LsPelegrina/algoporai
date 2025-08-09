package com.rinha.core.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String correlationId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {}