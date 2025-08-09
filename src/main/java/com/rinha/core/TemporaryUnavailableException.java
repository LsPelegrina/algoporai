package com.rinha.core;

public class TemporaryUnavailableException extends RuntimeException {
    public TemporaryUnavailableException(String message) {
        super(message);
    }
}