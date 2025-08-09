package com.rinha.core;

import com.rinha.core.model.ProcessorHealth;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class HealthCheckScheduler {

    private static final Logger LOGGER = Logger.getLogger(HealthCheckScheduler.class);

    @Inject
    @RestClient
    DefaultPaymentRestClient defaultRestClient;

    @Inject
    @RestClient
    FallbackPaymentRestClient fallbackRestClient;

    @Inject
    RedisRepository redisRepository;

    // **MUDANÇA CRÍTICA AQUI**: Hardcoded para remover dependência de property.
    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void checkProcessorsHealth() {
        if (redisRepository.tryAcquireHealthCheckLock()) {
            LOGGER.info("Acquired health check lock. Checking processors...");
            checkAndStoreHealth("default", defaultRestClient);
            checkAndStoreHealth("fallback", fallbackRestClient);
        } else {
            LOGGER.debug("Could not acquire health check lock. Another instance is running it.");
        }
    }

    private void checkAndStoreHealth(String processorName, PaymentRestClient restClient) {
        try {
            ProcessorHealth health = restClient.getHealth();
            redisRepository.updateProcessorHealth(processorName, health);
            LOGGER.debugf("Health check for %s: failing=%b, minResponseTime=%dms",
                    processorName, health.failing(), health.minResponseTime());
        } catch (ProcessingException e) {
            // **MUDANÇA CRÍTICA AQUI**: Em caso de timeout, apenas logamos e não alteramos o estado.
            // Isso evita marcar como UNHEALTHY por lentidão temporária na rede.
            LOGGER.error("Timeout during health check for " + processorName + " processor. State will not be changed.", e);
        } catch (Exception e) {
            // Para outras exceções (ex: conexão recusada), mantemos o comportamento defensivo.
            LOGGER.error("Failed to check health for " + processorName + " processor. Assuming unhealthy.", e);
            ProcessorHealth unhealthyStatus = new ProcessorHealth(true, 9999);
            redisRepository.updateProcessorHealth(processorName, unhealthyStatus);
        }
    }
}