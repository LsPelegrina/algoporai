package com.rinha.worker;

import com.rinha.core.PaymentProcessorService;
import com.rinha.core.RedisRepository;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Startup
@ApplicationScoped
public class QueueWorker {
    @Inject
    RedisRepository redis;
    @Inject
    PaymentProcessorService processorService;

    @Scheduled(every = "PT0.01S")
    void processBatch() {
        for (int i = 0; i < 256 /* ou mais */; i++) processOne();
    }

    private void processOne() {
        redis.dequeuePayment()
                .onItem().ifNotNull().transformToUni(req ->
                        processorService.process(req)
                                .flatMap(processor -> redis.incrementCounter(processor, req))
                                .onFailure().recoverWithUni(ex -> redis.enqueuePayment(req).map(ignore -> null))
                )
                .subscribe().with(x -> {}, err -> {/* logging de erro */});
    }
}
