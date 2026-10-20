package br.dev.guisleri.mototrack.exception;

public class TripDateUpdateRequiredException extends RuntimeException {
    public TripDateUpdateRequiredException(String message) {
        super(message);
    }
}
