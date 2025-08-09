package com.rinha.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@RegisterRestClient(configKey = "default-api")
public interface DefaultPaymentRestClient extends PaymentRestClient {}