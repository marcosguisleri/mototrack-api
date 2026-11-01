package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.Email;
import br.dev.guisleri.mototrack.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,

        @NotBlank(message = "O código é obrigatório.")
        @Pattern(regexp = "\\d{6}", message = "O código deve ter 6 dígitos.")
        String code,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
        @MaxUtf8Bytes(value = 72, message = "A senha deve ter no máximo 72 bytes (cerca de 72 caracteres simples; acentos e emojis ocupam mais).")
        String newPassword

) {

    public ResetPasswordRequestDTO {
        if (email != null) {
            email = email.trim();
        }
    }
}
