package com.rinha.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.PaymentSummary;
import com.rinha.core.model.ProcessorHealth;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RedisRepository {

    private static final Logger LOGGER = Logger.getLogger(RedisRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ListCommands<String, String> listCommands;
    private final ValueCommands<String, String> valueCommands;
    private final SortedSetCommands<String, String> sortedSetCommands;
    private final KeyCommands<String> keyCommands;

    @ConfigProperty(name = "app.redis.payment-queue-key")
    String paymentQueueKey;

    @ConfigProperty(name = "app.redis.payments-log-key")
    String paymentsLogKey;

    @ConfigProperty(name = "app.redis.health-ttl-seconds")
    long healthTtlSeconds;

    @ConfigProperty(name = "app.redis.health-default-key")
    String healthDefaultKey;

    @ConfigProperty(name = "app.redis.health-fallback-key")
    String healthFallbackKey;

    @ConfigProperty(name = "app.redis.health-check-lock-key")
    String healthCheckLockKey;

    @ConfigProperty(name = "app.redis.health-check-lock-timeout-seconds")
    long healthCheckLockTimeout;

    public RedisRepository(RedisDataSource ds) {
        this.listCommands = ds.list(String.class, String.class);
        this.valueCommands = ds.value(String.class, String.class);
        this.sortedSetCommands = ds.sortedSet(String.class, String.class);
        this.keyCommands = ds.key(String.class);
    }

    public void enqueuePayment(PaymentRequest request) {
        try {
            String jsonRequest = objectMapper.writeValueAsString(request);
            listCommands.lpush(paymentQueueKey, jsonRequest);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize payment request for queueing", e);
        }
    }

    public Optional<PaymentRequest> dequeueBlockingPayment() {
        try {
            KeyValue<String, String> result = listCommands.brpop(Duration.ofSeconds(5), paymentQueueKey);
            String jsonRequest = result != null ? result.value() : null;
            if (jsonRequest != null) {
                return Optional.of(objectMapper.readValue(jsonRequest, PaymentRequest.class));
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialize payment request from queue", e);
        }
        return Optional.empty();
    }

    public void updateProcessorHealth(String processorName, ProcessorHealth health) {
        try {
            String key = "default".equals(processorName) ? healthDefaultKey : healthFallbackKey;
            String jsonHealth = objectMapper.writeValueAsString(health);
            valueCommands.setex(key, healthTtlSeconds, jsonHealth);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize processor health", e);
        }
    }

    public Optional<ProcessorHealth> getProcessorHealth(String processorName) {
        try {
            String key = "default".equals(processorName) ? healthDefaultKey : healthFallbackKey;
            String jsonHealth = valueCommands.get(key);
            if (jsonHealth != null) {
                return Optional.of(objectMapper.readValue(jsonHealth, ProcessorHealth.class));
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialize processor health", e);
        }
        return Optional.empty();
    }

    public void recordSuccessfulPayment(String processorName, BigDecimal amount, Instant timestamp) {
        double score = timestamp.getEpochSecond() * 1_000_000 + timestamp.getNano() / 1_000.0;
        String value = String.format("%s:%.2f", processorName, amount);
        sortedSetCommands.zadd(paymentsLogKey, score, value);
    }

    public PaymentSummary getSummary(Instant from, Instant to) {
        long defaultRequests = 0;
        BigDecimal defaultAmount = BigDecimal.ZERO;
        long fallbackRequests = 0;
        BigDecimal fallbackAmount = BigDecimal.ZERO;

        double minScore = (from != null) ? from.getEpochSecond() * 1_000_000 + from.getNano() / 1_000.0 : Double.NEGATIVE_INFINITY;
        double maxScore = (to != null) ? to.getEpochSecond() * 1_000_000 + to.getNano() / 1_000.0 : Double.POSITIVE_INFINITY;

        ScoreRange<Double> scoreRange = new ScoreRange<>(minScore, maxScore);
        List<String> payments = sortedSetCommands.zrangebyscore(paymentsLogKey, scoreRange);

        for (String payment : payments) {
            String[] parts = payment.split(":");
            if (parts.length == 2) {
                String processor = parts[0];
                BigDecimal amount = new BigDecimal(parts[1]);
                if ("default".equals(processor)) {
                    defaultRequests++;
                    defaultAmount = defaultAmount.add(amount);
                } else if ("fallback".equals(processor)) {
                    fallbackRequests++;
                    fallbackAmount = fallbackAmount.add(amount);
                }
            }
        }

        return new PaymentSummary(
                new PaymentSummary.SummaryData(defaultRequests, defaultAmount),
                new PaymentSummary.SummaryData(fallbackRequests, fallbackAmount)
        );
    }

    public boolean tryAcquireHealthCheckLock() {
        SetArgs setArgs = new SetArgs().nx().ex(healthCheckLockTimeout);
        valueCommands.set(healthCheckLockKey, "locked", setArgs);
        return true;
    }

    public void purge() {
        keyCommands.del(paymentQueueKey, paymentsLogKey, healthDefaultKey, healthFallbackKey, healthCheckLockKey);
        LOGGER.info("Redis data purged.");
    }
}