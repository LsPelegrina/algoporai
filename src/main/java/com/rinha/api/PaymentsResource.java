package com.rinha.api;

import com.rinha.core.PaymentProcessorService;
import com.rinha.core.RedisRepository;
import com.rinha.core.model.PaymentRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/payments")
@ApplicationScoped
public class PaymentsResource {
    @Inject
    RedisRepository redis;
    @Inject
    PaymentProcessorService processorService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> process(PaymentRequest req) {
        if (req.getCorrelationId() == null || req.getAmount() == null)
            return Uni.createFrom().item(Response.status(400).build());

        // Enfileira processamento (LPUSH), resposta já é 2XX para não bloquear
        return redis.enqueuePayment(req)
                .replaceWith(Response.accepted().build());
    }
}

