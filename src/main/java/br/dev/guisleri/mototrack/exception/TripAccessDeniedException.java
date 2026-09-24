package br.dev.guisleri.mototrack.exception;

public class TripAccessDeniedException extends RuntimeException {
    public TripAccessDeniedException(String message) {
        super(message);
    }
}
