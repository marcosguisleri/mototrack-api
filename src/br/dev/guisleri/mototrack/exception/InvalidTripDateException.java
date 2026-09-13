package br.dev.guisleri.mototrack.exception;

public class InvalidTripDateException extends RuntimeException {
    public InvalidTripDateException(String message) {
        super(message);
    }
}
