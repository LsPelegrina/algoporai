package com.rinha.core;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "fallback-client")
public interface FallbackPaymentRestClient extends PaymentRestClient {}
