package com.rinha.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.core.RedisRepository;
import com.rinha.core.model.PaymentRequest;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/payments-summary")
@ApplicationScoped
public class SummaryResource {
    @Inject
    RedisRepository redis;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> get(@QueryParam("from") String from, @QueryParam("to") String to) {
        return redis.fetchSummary(from, to)
                .map(summary -> Response.ok(summary).build());
    }

    @PostConstruct
    void testeJackson() throws Exception {
        String exemplo = "{\"correlationId\":\"d0cdf046-e7bb-11ec-8fea-0242ac120002\",\"amount\":100.00}";
        ObjectMapper om = new ObjectMapper();
        PaymentRequest req = om.readValue(exemplo, PaymentRequest.class);
        System.out.println("DEBUG JACKSON: " + req.getCorrelationId() + " / " + req.getAmount());
    }

}
