package br.dev.guisleri.mototrack.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMotorcycleRequestDTO(

        @NotBlank
        String brand,

        @NotBlank
        String model,

        @NotBlank
        String color,

        @Min(1900)
        int year,

        @Positive
        int engineCapacity,

        @NotNull
        @Positive
        Long ownerId

) {
}
