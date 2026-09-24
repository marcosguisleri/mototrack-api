package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateMotorcycleRequestDTO(

        @NotBlank(message = "A marca é obrigatória.")
        String brand,

        @NotBlank(message = "O modelo é obrigatório.")
        String model,

        @NotBlank(message = "A cor é obrigatória.")
        String color,

        @Min(value = 1900, message = "O ano deve ser igual ou posterior a 1900.")
        int year,

        @Positive(message = "A cilindrada deve ser maior que zero.")
        int engineCapacity

) {
}
