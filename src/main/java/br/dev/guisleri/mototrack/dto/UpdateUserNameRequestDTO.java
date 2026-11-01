package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserNameRequestDTO(
        @NotBlank
        @Size(max = 100)
        String name
) {}
