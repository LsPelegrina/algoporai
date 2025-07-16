package com.rinha.core;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@ApplicationScoped
public class CircuitBreaker {
    private final AtomicInteger failures = new AtomicInteger(0);
    private volatile long openedAt = 0L;
    private static final int THRESHOLD = 3;
    private static final long TIMEOUT_MS = 4000;

    public <T> Uni<T> run(String processor, Supplier<Uni<T>> action) {
        if (isOpen()) return Uni.createFrom().failure(new RuntimeException("Circuit open"));
        return action.get().onFailure().invoke(this::registerFailure)
                .onItem().invoke(x -> reset());
    }
    private void registerFailure(Throwable thr) {
        if (failures.incrementAndGet() >= THRESHOLD)
            openedAt = System.currentTimeMillis();
    }
    private void reset() {
        failures.set(0);
        openedAt = 0L;
    }
    private boolean isOpen() {
        if (failures.get() < THRESHOLD) return false;
        return (System.currentTimeMillis() - openedAt) < TIMEOUT_MS;
    }
}

