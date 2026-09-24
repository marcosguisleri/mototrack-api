package br.dev.guisleri.mototrack.dto;

import java.util.Map;

public record ValidationErrorResponseDTO(
        int status,
        String error,
        Map<String, String> errors
) {
}
