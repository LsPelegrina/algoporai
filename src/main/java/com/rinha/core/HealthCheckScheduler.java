package com.rinha.core;

import com.rinha.core.model.ProcessorHealth;
import com.rinha.core.model.ProcessorStatus;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.impl.NoStackTraceTimeoutException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class HealthCheckScheduler {
    @Inject @RestClient DefaultPaymentRestClient defaultClient;
    @Inject @RestClient FallbackPaymentRestClient fallbackClient;
    @Inject ProcessorStatus status;

    @Scheduled(every="5s")
    void check() {
        defaultClient.health()
                .onFailure(NoStackTraceTimeoutException.class).recoverWithItem(() -> new ProcessorHealth(true, 0))  // Marca como failing em timeout
                .subscribe().with(resp -> status.update("default", !resp.failing()));

        fallbackClient.health()
                .onFailure(NoStackTraceTimeoutException.class).recoverWithItem(() -> new ProcessorHealth(true, 0))
                .subscribe().with(resp -> status.update("fallback", !resp.failing()));
    }

}

