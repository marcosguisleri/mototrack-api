package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.MotorcycleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateMotorcycleRequestDTO(

        @NotBlank(message = "A marca é obrigatória.")
        @Size(max = 255, message = "A marca deve ter no máximo 255 caracteres.")
        String brand,

        @NotBlank(message = "O modelo é obrigatório.")
        @Size(max = 255, message = "O modelo deve ter no máximo 255 caracteres.")
        String model,

        @NotBlank(message = "A cor é obrigatória.")
        @Size(max = 255, message = "A cor deve ter no máximo 255 caracteres.")
        String color,

        @Min(value = 1900, message = "O ano deve ser igual ou posterior a 1900.")
        int year,

        @Positive(message = "A cilindrada deve ser maior que zero.")
        int engineCapacity,

        @NotNull(message = "O tipo da motocicleta é obrigatório.")
        MotorcycleType type
) {
}
