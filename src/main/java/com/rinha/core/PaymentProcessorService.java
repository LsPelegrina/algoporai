package com.rinha.core;

import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.ProcessorHealth;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Serviço que decide dinamicamente qual payment processor usar,
 * consultando o cache de health compartilhado pelo Redis.
 */

@ApplicationScoped
public class PaymentProcessorService {
    @Inject RedisRepository redisRepository;
    @Inject @RestClient DefaultPaymentRestClient defaultClient;
    @Inject @RestClient FallbackPaymentRestClient fallbackClient;
    @Inject CircuitBreaker circuitBreaker;

    public Uni<String> process(PaymentRequest req) {
        Uni<ProcessorHealth> defaultHealthUni = redisRepository.getProcessorHealth("health:default");
        Uni<ProcessorHealth> fallbackHealthUni = redisRepository.getProcessorHealth("health:fallback");

        return Uni.combine().all().unis(defaultHealthUni, fallbackHealthUni).asTuple()
                .flatMap(tuple -> {
                    ProcessorHealth def = tuple.getItem1();
                    ProcessorHealth fbk = tuple.getItem2();
                    boolean defaultOk = def != null && !def.failing();
                    boolean fallbackOk = fbk != null && !fbk.failing();

                    if (defaultOk) {
                        return circuitBreaker.run("default", () -> defaultClient.process(req).map(v -> "default"))
                                .onFailure().recoverWithUni(x -> fallbackOk
                                        ? fallbackClient.process(req).map(v -> "fallback")
                                        : Uni.createFrom().failure(new TemporaryUnavailableException()));
                    } else if (fallbackOk) {
                        return circuitBreaker.run("fallback", () -> fallbackClient.process(req).map(v -> "fallback"));
                    } else {
                        return Uni.createFrom().failure(new TemporaryUnavailableException());
                    }
                });
    }
}

