package br.dev.guisleri.mototrack.exception;

public class InvalidEmailChangeException extends RuntimeException {
    public InvalidEmailChangeException(String message) {
        super(message);
    }
}
