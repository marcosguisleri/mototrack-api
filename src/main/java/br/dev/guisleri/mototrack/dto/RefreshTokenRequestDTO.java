package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(

        @NotBlank(message = "O refresh token é obrigatório.")
        String refreshToken

) {
}
