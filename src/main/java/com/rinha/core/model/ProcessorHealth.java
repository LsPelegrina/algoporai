package com.rinha.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcessorHealth(
        @JsonProperty("failing") boolean failing,
        @JsonProperty("minResponseTime") int minResponseTime
) {}