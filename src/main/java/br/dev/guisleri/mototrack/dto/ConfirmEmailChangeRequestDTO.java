package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmEmailChangeRequestDTO(

        @NotBlank(message = "O código é obrigatório.")
        @Pattern(regexp = "\\d{6}", message = "O código deve ter 6 dígitos.")
        String code

) {
}
