package com.rinha.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RegisterForReflection
public class PaymentRequest {
    @JsonProperty("correlationId")
    private UUID correlationId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("requestedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", shape = JsonFormat.Shape.STRING)
    private OffsetDateTime requestedAt;

    public PaymentRequest() {}

    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID id) { this.correlationId = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amt) { this.amount = amt; }
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(OffsetDateTime requestedAt) { this.requestedAt = requestedAt; }
}
