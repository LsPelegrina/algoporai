package com.rinha.core.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ProcessorHealth(
        boolean failing,
        int minResponseTime
) {}
