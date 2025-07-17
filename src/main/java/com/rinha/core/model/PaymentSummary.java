package com.rinha.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

@RegisterForReflection
public record PaymentSummary(
        @JsonProperty("default")
        Map<String, Object> defaultProcessor,
        @JsonProperty("fallback")
        Map<String, Object> fallbackProcessor
) {}
