package com.rinha.worker;

import com.rinha.core.PaymentProcessorService;
import com.rinha.core.RedisRepository;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Startup
@Singleton
public class QueueWorker {

    @Inject
    RedisRepository redis;

    @Inject
    PaymentProcessorService processorService;

    @Scheduled(every = "PT1S")
    void popAndProcess() {
        redis.dequeuePayment()
                .onItem().ifNotNull().transformToUni(req -> {
                    Log.info("Processando pagamento: " + req.getCorrelationId());
                    return processorService.process(req)
                            .map(processor -> redis.incrementCounter(processor, req));
                })
                .subscribe().with(x -> {}, err -> Log.error("Erro no worker: " + err.getMessage()));
    }
}
