package com.rinha.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.PaymentSummary;
import com.rinha.core.model.ProcessorHealth;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Redis;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.vertx.mutiny.redis.client.Response;

import java.math.BigDecimal;
import java.util.Map;

@ApplicationScoped
public class RedisRepository {

    @Inject
    Redis redis;
    @Inject
    ObjectMapper objectMapper;

    private String serialize(PaymentRequest req) {
        try {
            return objectMapper.writeValueAsString(req);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar PaymentRequest", e);
        }
    }

    private PaymentRequest deserialize(String json) {
        try {
            return objectMapper.readValue(json, PaymentRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deserializar PaymentRequest", e);
        }
    }

    private long parseLong(Object val) { try { return val != null ? Long.parseLong(val.toString()) : 0L; } catch (Exception e) { return 0L; } }
    private BigDecimal parseBigDecimal(Object val) { try { return val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO; } catch (Exception e) { return BigDecimal.ZERO; } }


    private PaymentSummary parseSummary(Response response) {
        long defaultCount = parseLong(response.get("default:count"));
        BigDecimal defaultAmount = parseBigDecimal(response.get("default:amount"));
        long fallbackCount = parseLong(response.get("fallback:count"));
        BigDecimal fallbackAmount = parseBigDecimal(response.get("fallback:amount"));

        return new PaymentSummary(
                Map.of("totalRequests", defaultCount, "totalAmount", defaultAmount),
                Map.of("totalRequests", fallbackCount, "totalAmount", fallbackAmount)
        );
    }





    public Uni<Void> enqueuePayment(PaymentRequest req) {
        Request request = Request.cmd(Command.LPUSH).arg("payments-queue").arg(serialize(req));
        return redis.send(request).map(response -> null);
    }

    public Uni<PaymentSummary> fetchSummary(String from, String to) {
        Request request = Request.cmd(Command.HGETALL).arg("summary");
        return redis.send(request).map(this::parseSummary);
    }

    public Uni<PaymentRequest> dequeuePayment() {
        Request request = Request.cmd(Command.RPOP).arg("payments-queue");
        return redis.send(request).map(response -> response != null ? deserialize(response.toString()) : null);
    }

    public Uni<Void> incrementCounter(String processor, PaymentRequest req) {
        return redis.send(Request.cmd(Command.HINCRBY).arg("summary").arg(processor + ":count").arg("1"))
                .flatMap(v ->
                        redis.send(Request.cmd(Command.HINCRBYFLOAT).arg("summary").arg(processor + ":amount").arg(req.getAmount().toString()))
                )
                .map(v -> null);
    }

    public Uni<ProcessorHealth> getProcessorHealth(String cacheKey) {
        return redis.send(Request.cmd(Command.GET).arg(cacheKey))
                .map(r -> {
                    if (r == null) return new ProcessorHealth(true, -1); // Se não há cache, assume falha
                    try {
                        return new ObjectMapper().readValue(r.toString(), ProcessorHealth.class);
                    } catch (Exception e) { return new ProcessorHealth(true, -1); }
                });
    }

    public Uni<Void> retryLater(PaymentRequest req) {
        // Empurra para uma fila de retry ou para o final da fila 'payments-queue'
        // Se quiser, pode usar ZSET (score de próximo retry) para um flow mais sofisticado.
        return Uni.createFrom().voidItem()
                .onItem().invoke(() -> {
                    // Pequeno delay opcional (ex: Thread.sleep ou delay entre retiros)
                })
                .chain(() -> enqueuePayment(req));
    }

}