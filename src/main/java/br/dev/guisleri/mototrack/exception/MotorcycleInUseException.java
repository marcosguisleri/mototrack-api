package br.dev.guisleri.mototrack.exception;

public class MotorcycleInUseException extends RuntimeException {
    public MotorcycleInUseException(String message) {
        super(message);
    }
}
