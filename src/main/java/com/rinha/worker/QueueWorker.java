package com.rinha.worker;

import com.rinha.core.PaymentAuditRepository;
import com.rinha.core.PaymentProcessorService;
import com.rinha.core.RedisRepository;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class QueueWorker {
    @Inject
    RedisRepository redis;
    @Inject
    PaymentProcessorService processorService;
    @Inject
    PaymentAuditRepository paymentAuditRepository;

    @Scheduled(every = "PT0.01S")
    void processBatch() {
        for (int i = 0; i < 301 /* ou mais */; i++) processOne();
    }

    private void processOne() {
        redis.dequeuePayment()
                .onItem().ifNotNull().transformToUni(req ->
                        redis.tryLock(req.getCorrelationId().toString())
                                .flatMap(lock -> {
                                    if (!lock) return Uni.createFrom().voidItem();
                                    return processorService.process(req)
                                            .flatMap(processor -> paymentAuditRepository.auditPayment(req, processor));
                                })
                                .onFailure().recoverWithUni(ex -> redis.enqueuePayment(req).map(ignore -> null))
                )
                .subscribe().with(result -> {}, error -> {});
    }

}