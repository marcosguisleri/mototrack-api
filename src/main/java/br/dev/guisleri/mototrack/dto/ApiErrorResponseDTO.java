package br.dev.guisleri.mototrack.dto;

import java.util.Map;

public record ApiErrorResponseDTO(
        int status,
        String error,
        String message,
        Map<String, String> errors
) {
}
