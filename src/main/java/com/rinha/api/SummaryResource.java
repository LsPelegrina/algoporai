package com.rinha.api;

import com.rinha.core.RedisRepository;
import io.smallrye.mutiny.Uni;
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
}
