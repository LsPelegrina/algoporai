package com.rinha.api;

import com.rinha.core.RedisRepository;
import com.rinha.core.model.PaymentRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/payments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentsResource {

    @Inject
    RedisRepository redisRepository;

    @POST
    public Response createPayment(@Valid PaymentRequest paymentRequest) {
        redisRepository.enqueuePayment(paymentRequest);
        return Response.accepted().build();
    }
}