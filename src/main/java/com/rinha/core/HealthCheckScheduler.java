package com.rinha.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.core.model.ProcessorHealth;
import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class HealthCheckScheduler {
    private static final String LOCK_KEY = "healthcheck-lock";
    private static final int LOCK_TTL_SECONDS = 5;
    private static final String DEFAULT_CACHE_KEY = "health:default";
    private static final String FALLBACK_CACHE_KEY = "health:fallback";

    @Inject @RestClient DefaultPaymentRestClient defaultClient;
    @Inject @RestClient FallbackPaymentRestClient fallbackClient;
    @Inject
    Redis redis;

    @Scheduled(every="5s")
    void scheduledHealthCheck() {
        if (!tryAcquireHealthCheckLock(LOCK_KEY, LOCK_TTL_SECONDS)) {
            return;
        }

        defaultClient.health()
                .onFailure().recoverWithItem(() -> new ProcessorHealth(true, -1))
                .subscribe().with(resp -> saveProcessorCache(DEFAULT_CACHE_KEY, resp));

        fallbackClient.health()
                .onFailure().recoverWithItem(() -> new ProcessorHealth(true, -1))
                .subscribe().with(resp -> saveProcessorCache(FALLBACK_CACHE_KEY, resp));
    }

    private boolean tryAcquireHealthCheckLock(String lockKey, int secondsTTL) {
        String script = "return redis.call('SET', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) and 1 or 0;";
        String myId = "api-" + java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        Response resp = redis.send(Request.cmd(Command.EVAL)
                        .arg(script)
                        .arg(1).arg(lockKey).arg(myId).arg(Integer.toString(secondsTTL)))
                .await().indefinitely();
        return resp != null && resp.toInteger() == 1;
    }

    private void saveProcessorCache(String cacheKey, ProcessorHealth health) {
        try {
            String value = new ObjectMapper().writeValueAsString(health);
            redis.send(Request.cmd(Command.SETEX).arg(cacheKey).arg("7").arg(value)).subscribe().with(r -> {});
        } catch (Exception e) {
            // log erro se necessário
        }
    }
}