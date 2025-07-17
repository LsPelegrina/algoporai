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

    @Scheduled(every = "0.01s")
    void popAndProcessA() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessB() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessC() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessD() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessE() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessF() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessG() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessH() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessI() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessJ() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessK() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessL() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessM() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessN() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessO() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessP() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessQ() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessR() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessS() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessT() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessU() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessV() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessW() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessX() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessY() {
        processOne();
    }

    @Scheduled(every = "0.01s")
    void popAndProcessZ() {
        processOne();
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
