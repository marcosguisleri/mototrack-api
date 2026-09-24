package br.dev.guisleri.mototrack.exception;

public class MotorcycleAccessDeniedException extends RuntimeException {
    public MotorcycleAccessDeniedException(String message) {
        super(message);
    }
}
