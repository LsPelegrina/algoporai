package com.rinha.core;

import com.rinha.core.model.PaymentRequest;
import com.rinha.core.model.ProcessorStatus;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class PaymentProcessorService {
    @Inject ProcessorStatus status;
    @Inject @RestClient DefaultPaymentRestClient defaultClient;
    @Inject @RestClient FallbackPaymentRestClient fallbackClient;
    @Inject CircuitBreaker cb;

    public Uni<String> process(PaymentRequest req) {
        String target = status.choose();
        PaymentRestClient client = "default".equals(target) ? defaultClient : fallbackClient;

        return cb.run(target, () -> client.process(req).map(v -> target))
                .onFailure().recoverWithUni(ex -> fallbackClient.process(req).map(v -> "fallback"));
    }
}

