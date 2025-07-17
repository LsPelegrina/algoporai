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
                    return processorService.process(req)
                            .onItem().invoke(processor -> {
                                // Processado com sucesso por "processor"
                                redis.incrementCounter(processor, req).subscribe().with(x -> {
                                });
                            })
                            .onFailure().recoverWithUni(ex -> {
                                // Nenhum processor disponível. Re-enfileirar com backoff.
                                redis.retryLater(req).subscribe().with(x -> {
                                });
                                return Uni.createFrom().nullItem();
                            });
                })
                .subscribe().with(x -> {
                }, err -> Log.error("Erro no worker: " + err.getMessage()));
    }
}