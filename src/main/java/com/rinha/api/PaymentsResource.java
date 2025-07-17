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
    public Uni<Response> process(PaymentRequest req) {
        System.out.println("DEBUG :: PaymentRequest recebido: " + req); // Mostra se req é null
        if (req == null) {
            System.out.println("DEBUG :: PaymentRequest desserializado é NULL");
            return Uni.createFrom().item(Response.status(400).entity("Body inválido!").build());
        }
        System.out.println("DEBUG :: correlationId=" + req.getCorrelationId() + ", amount=" + req.getAmount());
        // ...
        return redis.enqueuePayment(req)
                .replaceWith(Response.accepted().build());
    }
}
