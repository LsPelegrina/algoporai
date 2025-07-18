package com.rinha.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.core.PaymentProcessorService;
import com.rinha.core.RedisRepository;
import com.rinha.core.model.PaymentRequest;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Path("/payments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
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

        // Gera timestamp no backend no formato correto, 3 casas decimais, UTC
        req.setRequestedAt(OffsetDateTime.now(ZoneOffset.UTC).withNano(0));

        return redis.enqueuePayment(req)
                .replaceWith(Response.accepted().build());
    }
}
