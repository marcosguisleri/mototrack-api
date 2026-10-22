package br.dev.guisleri.mototrack.exception;

public class TripExpenseNotFoundException extends RuntimeException {
    public TripExpenseNotFoundException(String message) {
        super(message);
    }
}
