package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.Email;
import br.dev.guisleri.mototrack.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um endereço de e-mail válido.")
        String email,


        @NotBlank(message = "A senha é obrigatória.")
        @MaxUtf8Bytes(value = 72, message = "A senha deve ter no máximo 72 bytes (cerca de 72 caracteres simples; acentos e emojis ocupam mais).")
        String password

) {
}
