package com.rinha.api;

import com.rinha.core.PaymentAuditRepository;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.time.OffsetDateTime;

@Path("/payments-summary")
@Produces(MediaType.APPLICATION_JSON)
public class SummaryResource {

    @Inject
    PaymentAuditRepository paymentAuditRepository;

    @GET
    public Uni<Response> getSummary(@QueryParam("from") String from, @QueryParam("to") String to) {
        OffsetDateTime fromTime = from != null ? OffsetDateTime.parse(from) : OffsetDateTime.MIN;
        OffsetDateTime toTime   = to   != null ? OffsetDateTime.parse(to) : OffsetDateTime.now();
        return paymentAuditRepository.summary(fromTime, toTime)
                .map(summary -> Response.ok(summary).build());
    }
}
