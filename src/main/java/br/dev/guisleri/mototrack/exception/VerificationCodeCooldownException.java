package br.dev.guisleri.mototrack.exception;

public class VerificationCodeCooldownException extends RuntimeException {
    public VerificationCodeCooldownException(String message) {
        super(message);
    }
}
