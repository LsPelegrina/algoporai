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

    public PaymentRequest() {}
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID id) { this.correlationId = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "correlationId=" + correlationId + ", amount=" + amount + '}';
    }
}


