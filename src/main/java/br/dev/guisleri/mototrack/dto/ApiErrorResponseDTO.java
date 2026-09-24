package br.dev.guisleri.mototrack.dto;

public record ApiErrorResponseDTO(
        int status,
        String error,
        String message
) {
}
