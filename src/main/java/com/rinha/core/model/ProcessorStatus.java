package com.rinha.core.model;

import jakarta.inject.Singleton;

@Singleton
public class ProcessorStatus {
    private volatile boolean defaultHealthy = true;
    private volatile boolean fallbackHealthy = true;

    public synchronized String choose() {
        if (defaultHealthy) return "default";
        if (fallbackHealthy) return "fallback";
        return "fallback";
    }
    public void update(String processor, boolean healthy) {
        if ("default".equals(processor)) defaultHealthy = healthy;
        else fallbackHealthy = healthy;
    }
}
