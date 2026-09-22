package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequestDTO(

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email

) {

    public CreateUserRequestDTO {
        if (email != null) {
            email = email.trim();
        }
    }
}