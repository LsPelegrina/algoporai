package com.rinha.core.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

@RegisterForReflection
public record PaymentSummary(
        Map<String, Object> defaultProcessor,
        Map<String, Object> fallbackProcessor
) {}
