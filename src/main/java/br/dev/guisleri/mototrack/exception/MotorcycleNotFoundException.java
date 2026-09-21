package br.dev.guisleri.mototrack.exception;

public class MotorcycleNotFoundException extends RuntimeException {
    public MotorcycleNotFoundException(String message) {
        super(message);
    }
}
