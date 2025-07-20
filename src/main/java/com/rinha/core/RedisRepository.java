package com.rinha.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.PaymentSummary;
import com.rinha.core.model.ProcessorHealth;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Response;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@ApplicationScoped
public class RedisRepository {

    @Inject
    Redis redis;
    @Inject
    ObjectMapper objectMapper;

    private String acquireLockLuaSha = null;
    private static final int LOCK_TTL_SEC = 180;

    @PostConstruct
    void loadLuaScripts() {
        try {
            String script = Files.readString(Path.of("/app/scripts/acquire_lock.lua"));
            Response resp = redis.send(Request.cmd(Command.SCRIPT).arg("LOAD").arg(script)).await().indefinitely();
            this.acquireLockLuaSha = resp.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar script Lua no Redis", e);
        }
    }

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
        return redis.send(Request.cmd(Command.HINCRBY).arg("summary").arg(processor + ":count").arg(1))
                .flatMap(v ->
                        redis.send(Request.cmd(Command.HINCRBYFLOAT).arg("summary").arg(processor + ":amount").arg(req.getAmount().toString()))
                )
                .map(v -> null);
    }

    public Uni<ProcessorHealth> getProcessorHealth(String cacheKey) {
        return redis.send(Request.cmd(Command.GET).arg(cacheKey))
                .map(r -> {
                    if (r == null) return new ProcessorHealth(true, -1);
                    try {
                        return objectMapper.readValue(r.toString(), ProcessorHealth.class);
                    } catch (Exception e) { return new ProcessorHealth(true, -1); }
                });
    }

    /** Try to acquire idempotency lock for a payment using Lua script with TTL (180s). */
    public Uni<Boolean> tryLock(String correlationId) {
        String key = "payment:lock:" + correlationId;
        if (acquireLockLuaSha != null) {
            // Usa o script carregado (mais eficiente)
            return redis.send(Request.cmd(Command.EVALSHA)
                    .arg(acquireLockLuaSha).arg(1).arg(key).arg("1").arg(String.valueOf(LOCK_TTL_SEC))
            ).map(resp -> resp != null && resp.toInteger() == 1);
        } else {
            // Fallback: executa o script inline na primeira vez
            String lua = "if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) then return 1 else return 0 end";
            return redis.send(Request.cmd(Command.EVAL)
                    .arg(lua).arg(1).arg(key).arg("1").arg(String.valueOf(LOCK_TTL_SEC))
            ).map(resp -> resp != null && resp.toInteger() == 1);
        }
    }
}
