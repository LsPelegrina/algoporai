package com.rinha.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.core.RedisRepository;
import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.PaymentSummary;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@Path("/payments-summary")
@ApplicationScoped
public class SummaryResource {
    @Inject
    RedisRepository redis;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> get(@QueryParam("from") String from, @QueryParam("to") String to) {
        return redis.fetchSummary(from, to)
                .map(summary -> {
                    if (summary == null)
                        return Response.ok(
                                new PaymentSummary(
                                        Map.of("totalRequests", 0, "totalAmount", BigDecimal.ZERO),
                                        Map.of("totalRequests", 0, "totalAmount", BigDecimal.ZERO)
                                )
                        ).build();
                    return Response.ok(summary).build();
                });
    }
}
