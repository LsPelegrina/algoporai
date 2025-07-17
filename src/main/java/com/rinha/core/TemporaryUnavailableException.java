package com.rinha.core;

/**
 * Exceção de controle para indicar que ambos os payment processors estão
 * temporariamente indisponíveis. Permite lógica de retry/re-enfileiramento.
 */
public class TemporaryUnavailableException extends RuntimeException {
    public TemporaryUnavailableException() {
        super("Ambos os payment processors estão temporariamente indisponíveis.");
    }

    public TemporaryUnavailableException(String message) {
        super(message);
    }
}
