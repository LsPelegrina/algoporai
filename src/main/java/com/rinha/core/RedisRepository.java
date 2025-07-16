package com.rinha.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.PaymentSummary;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Redis;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
            return objectMapper.writeValueAsString(req);  // Serializa para JSON
        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar PaymentRequest", e);
        }
    }

    private PaymentRequest deserialize(String json) {
        try {
            return objectMapper.readValue(json, PaymentRequest.class);  // Deserializa do JSON
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deserializar PaymentRequest", e);
        }
    }

    private PaymentSummary parseSummary(Response response) {
        long defaultCount = response.get("default:count") != null ? Long.parseLong(response.get("default:count").toString()) : 0;
        BigDecimal defaultAmount = response.get("default:amount") != null ? new BigDecimal(response.get("default:amount").toString()) : BigDecimal.ZERO;
        long fallbackCount = response.get("fallback:count") != null ? Long.parseLong(response.get("fallback:count").toString()) : 0;
        BigDecimal fallbackAmount = response.get("fallback:amount") != null ? new BigDecimal(response.get("fallback:amount").toString()) : BigDecimal.ZERO;

        return new PaymentSummary(Map.of("default:count", defaultCount, "default:amount", defaultAmount),
                Map.of("fallback:count", fallbackCount, "fallback:amount", fallbackAmount));
    }

    public Uni<Void> enqueuePayment(PaymentRequest req) {
        Request request = Request.cmd(Command.LPUSH).arg("payments-queue").arg(serialize(req));
        return redis.send(request).map(response -> null);
    }

    public Uni<PaymentSummary> fetchSummary(String from, String to) {
        Request request = Request.cmd(Command.HGETALL).arg("summary");
        return redis.send(request).map(response -> parseSummary(response));
    }

    public Uni<PaymentRequest> dequeuePayment() {
        Request request = Request.cmd(Command.RPOP).arg("payments-queue");
        return redis.send(request).map(response -> response != null ? deserialize(response.toString()) : null);
    }

    public Uni<Void> incrementCounter(String processor, PaymentRequest req) {
        return redis.send(Request.cmd(Command.MULTI))
                .flatMap(v -> redis.send(Request.cmd(Command.HINCRBY).arg("summary").arg(processor + ":count").arg("1")))
                .flatMap(v -> redis.send(Request.cmd(Command.HINCRBYFLOAT).arg("summary").arg(processor + ":amount").arg(req.getAmount().toString())))
                .flatMap(v -> redis.send(Request.cmd(Command.EXEC)))
                .map(v -> null);
    }
}