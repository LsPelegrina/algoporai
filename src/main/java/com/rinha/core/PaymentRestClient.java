package com.rinha.core;

import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.ProcessorHealth;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient
public interface PaymentRestClient {
    @POST
    @Path("/payments")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Void> process(PaymentRequest req);

    @GET
    @Path("/payments/service-health")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<ProcessorHealth> health();
}
