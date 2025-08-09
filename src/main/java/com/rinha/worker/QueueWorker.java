package com.rinha.worker;

import com.rinha.core.PaymentProcessorService;
import com.rinha.core.RedisRepository;
import com.rinha.core.TemporaryUnavailableException;
import com.rinha.core.model.PaymentRequest;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class QueueWorker {

    private static final Logger LOGGER = Logger.getLogger(QueueWorker.class);
    // **MUDANÇA CRÍTICA AQUI**: Aumenta a capacidade de processamento paralelo.
    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    @Inject
    RedisRepository redisRepository;

    @Inject
    PaymentProcessorService paymentProcessorService;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Starting QueueWorker with a 4-thread processing pool...");
        // Inicia um consumidor para cada thread no pool
        for (int i = 0; i < 4; i++) {
            executor.submit(this::processQueue);
        }
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Optional<PaymentRequest> paymentRequestOpt = redisRepository.dequeueBlockingPayment();
                paymentRequestOpt.ifPresent(this::handlePayment);
            } catch (Exception e) {
                LOGGER.error("Error in worker loop, continuing...", e);
                sleep(1000); // Avoid fast spinning on unexpected errors
            }
        }
    }

    private void handlePayment(PaymentRequest paymentRequest) {
        try {
            paymentProcessorService.processPayment(paymentRequest);
        } catch (TemporaryUnavailableException e) {
            LOGGER.warnf("Re-queuing payment %s due to: %s", paymentRequest.correlationId(), e.getMessage());
            redisRepository.enqueuePayment(paymentRequest);
            sleep(100); // Backoff to avoid busy-looping when all services are down
        } catch (Exception e) {
            LOGGER.errorf(e, "Failed to process payment %s. Dropping from queue to avoid poison pill.", paymentRequest.correlationId());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}