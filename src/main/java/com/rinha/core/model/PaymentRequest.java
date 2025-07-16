package com.rinha.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;
import java.util.UUID;

@RegisterForReflection
public class PaymentRequest {

    @JsonProperty("correlationId")
    private UUID correlationId;

    @JsonProperty("amount")
    private BigDecimal amount;

    public PaymentRequest() {}                        // construtor vazio requerido pelo Jackson
    public PaymentRequest(UUID correlationId, BigDecimal amount) {
        this.correlationId = correlationId;
        this.amount = amount;
    }

    public UUID getCorrelationId() {                 // <-- Getter
        return correlationId;
    }
    public BigDecimal getAmount() {                  // <-- Getter
        return amount;
    }
}

