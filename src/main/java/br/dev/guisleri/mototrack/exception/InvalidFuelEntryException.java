package br.dev.guisleri.mototrack.exception;

public class InvalidFuelEntryException extends RuntimeException {
    public InvalidFuelEntryException(String message) {
        super(message);
    }
}
