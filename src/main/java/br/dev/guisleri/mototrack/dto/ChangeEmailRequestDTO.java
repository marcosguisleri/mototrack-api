package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.Email;
import br.dev.guisleri.mototrack.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequestDTO(

        @NotBlank(message = "O novo e-mail é obrigatório.")
        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String newEmail,

        @NotBlank(message = "A senha atual é obrigatória.")
        @MaxUtf8Bytes(value = 72, message = "A senha deve ter no máximo 72 bytes (cerca de 72 caracteres simples; acentos e emojis ocupam mais).")
        String currentPassword

) {

    public ChangeEmailRequestDTO {
        if (newEmail != null) {
            newEmail = newEmail.trim();
        }
    }
}
