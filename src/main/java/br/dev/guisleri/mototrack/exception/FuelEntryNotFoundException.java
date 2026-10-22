package br.dev.guisleri.mototrack.exception;

public class FuelEntryNotFoundException extends RuntimeException {
    public FuelEntryNotFoundException(String message) {
        super(message);
    }
}
