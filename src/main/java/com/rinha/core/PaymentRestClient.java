package com.rinha.core;

import com.rinha.core.model.ProcessorHealth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;

public interface PaymentRestClient {

    @POST
    @Path("/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    void processPayment(PaymentProcessorRequest request);

    @GET
    @Path("/payments/service-health")
    @Produces(MediaType.APPLICATION_JSON)
    ProcessorHealth getHealth();

    record PaymentProcessorRequest(
            String correlationId,
            BigDecimal amount,
            String requestedAt
    ) {}
}