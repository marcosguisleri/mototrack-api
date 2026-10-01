package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um endereço de e-mail válido.")
        String email,


        @NotBlank(message = "A senha é obrigatória.")
        String password

) {
}
