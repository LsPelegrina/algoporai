package com.rinha.api;

import com.rinha.core.RedisRepository;
import com.rinha.core.model.PaymentSummary;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;

@Path("/payments-summary")
@Produces(MediaType.APPLICATION_JSON)
public class SummaryResource {

    @Inject
    RedisRepository redisRepository;

    @GET
    public PaymentSummary getSummary(@QueryParam("from") String from, @QueryParam("to") String to) {
        Instant fromInstant = (from!= null)? Instant.parse(from) : null;
        Instant toInstant = (to!= null)? Instant.parse(to) : null;
        return redisRepository.getSummary(fromInstant, toInstant);
    }
}