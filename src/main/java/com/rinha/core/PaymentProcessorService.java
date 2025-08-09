package com.rinha.core;

import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.ProcessorHealth;
import com.rinha.core.model.ProcessorStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class PaymentProcessorService {

    private static final Logger LOGGER = Logger.getLogger(PaymentProcessorService.class);
    private static final int DEGRADED_THRESHOLD_MS = 200;

    @Inject
    @RestClient
    DefaultPaymentRestClient defaultRestClient;

    @Inject
    @RestClient
    FallbackPaymentRestClient fallbackRestClient;

    @Inject
    RedisRepository redisRepository;

    public void processPayment(PaymentRequest paymentRequest) {
        Optional<String> chosenProcessor = chooseProcessor();

        if (chosenProcessor.isEmpty()) {
            LOGGER.warn("Both processors are unavailable. Re-queuing payment.");
            throw new TemporaryUnavailableException("All payment processors are currently unavailable.");
        }

        String processorName = chosenProcessor.get();
        PaymentRestClient client = getRestClient(processorName);
        Instant requestTimestamp = Instant.now();

        try {
            client.processPayment(new PaymentRestClient.PaymentProcessorRequest(
                    paymentRequest.correlationId(),
                    paymentRequest.amount(),
                    requestTimestamp.toString()
            ));
            // Passa o timestamp para o registro
            redisRepository.recordSuccessfulPayment(processorName, paymentRequest.amount(), requestTimestamp);
            LOGGER.debugf("Payment %s processed successfully by %s", paymentRequest.correlationId(), processorName);
        } catch (Exception e) {
            LOGGER.errorf(e, "Failed to process payment %s with processor %s", paymentRequest.correlationId(), processorName);
            throw new TemporaryUnavailableException("Failed to process payment with " + processorName);
        }
    }

    private Optional<String> chooseProcessor() {
        ProcessorStatus defaultStatus = getProcessorStatus("default");
        ProcessorStatus fallbackStatus = getProcessorStatus("fallback");

        if (defaultStatus == ProcessorStatus.HEALTHY) {
            return Optional.of("default");
        }

        if (fallbackStatus == ProcessorStatus.HEALTHY) {
            LOGGER.warn("Default processor is unavailable/degraded. Switching to healthy fallback.");
            return Optional.of("fallback");
        }

        if (defaultStatus == ProcessorStatus.DEGRADED) {
            LOGGER.warn("Fallback is unhealthy. Attempting degraded default processor as a last resort.");
            return Optional.of("default");
        }

        if (fallbackStatus == ProcessorStatus.DEGRADED) {
            LOGGER.warn("Default is unhealthy. Attempting degraded fallback processor.");
            return Optional.of("fallback");
        }

        LOGGER.error("Both processors are unhealthy. No processor chosen.");
        return Optional.empty();
    }

    private ProcessorStatus getProcessorStatus(String processorName) {
        Optional<ProcessorHealth> healthOpt = redisRepository.getProcessorHealth(processorName);

        if (healthOpt.isEmpty()) {
            LOGGER.warnf("No health information for processor %s, assuming HEALTHY.", processorName);
            return ProcessorStatus.HEALTHY;
        }

        ProcessorHealth health = healthOpt.get();
        if (health.failing()) {
            return ProcessorStatus.UNHEALTHY;
        }

        if (health.minResponseTime() > DEGRADED_THRESHOLD_MS) {
            return ProcessorStatus.DEGRADED;
        }

        return ProcessorStatus.HEALTHY;
    }

    private PaymentRestClient getRestClient(String processorName) {
        return "default".equals(processorName)? defaultRestClient : fallbackRestClient;
    }
}