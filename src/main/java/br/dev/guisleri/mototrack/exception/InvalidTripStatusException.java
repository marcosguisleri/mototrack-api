package br.dev.guisleri.mototrack.exception;

public class InvalidTripStatusException extends RuntimeException {
    public InvalidTripStatusException(String message) {
        super(message);
    }
}
