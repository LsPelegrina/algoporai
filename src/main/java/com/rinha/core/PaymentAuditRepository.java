package com.rinha.core;

import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.PaymentSummary;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@ApplicationScoped
public class PaymentAuditRepository {

    @Inject
    PgPool pgPool;

    public Uni<Void> auditPayment(PaymentRequest req, String processor) {
        // "ON CONFLICT DO NOTHING" impede duplicidade por correlationId
        String sql = "INSERT INTO payment_audit (correlation_id, amount, processor, requested_at) " +
                "VALUES ($1, $2, $3, $4) ON CONFLICT DO NOTHING";
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(req.getCorrelationId(), req.getAmount(), processor, req.getRequestedAt()))
                .map(rs -> null);
    }

    public Uni<PaymentSummary> summary(OffsetDateTime from, OffsetDateTime to) {
        String sql = """
            SELECT processor, COUNT(*) AS total_requests, COALESCE(SUM(amount), 0) AS total_amount
            FROM payment_audit
            WHERE requested_at >= $1 AND requested_at <= $2
            GROUP BY processor
            """;
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(from, to))
                .map(rs -> {
                    long defaultCount = 0, fallbackCount = 0;
                    BigDecimal defaultAmount = BigDecimal.ZERO, fallbackAmount = BigDecimal.ZERO;
                    for (Row row : rs) {
                        String proc = row.getString("processor");
                        if ("default".equals(proc)) {
                            defaultCount = row.getLong("total_requests");
                            defaultAmount = row.getBigDecimal("total_amount");
                        } else if ("fallback".equals(proc)) {
                            fallbackCount = row.getLong("total_requests");
                            fallbackAmount = row.getBigDecimal("total_amount");
                        }
                    }
                    return new PaymentSummary(
                            Map.of("totalRequests", defaultCount, "totalAmount", defaultAmount),
                            Map.of("totalRequests", fallbackCount, "totalAmount", fallbackAmount)
                    );
                });
    }
}
