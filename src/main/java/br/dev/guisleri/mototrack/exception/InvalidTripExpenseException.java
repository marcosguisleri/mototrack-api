package br.dev.guisleri.mototrack.exception;

public class InvalidTripExpenseException extends RuntimeException {
    public InvalidTripExpenseException(String message) {
        super(message);
    }
}
