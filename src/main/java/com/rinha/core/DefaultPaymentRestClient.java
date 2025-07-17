package com.rinha.core;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "default-client")
public interface DefaultPaymentRestClient extends PaymentRestClient {}
