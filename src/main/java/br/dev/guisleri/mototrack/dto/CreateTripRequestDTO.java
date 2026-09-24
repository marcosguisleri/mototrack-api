package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TerrainType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateTripRequestDTO(

        @NotBlank(message = "A origem é obrigatória.")
        String origin,

        @NotBlank(message = "O destino é obrigatório.")
        String destination,

        @NotNull(message = "A distância é obrigatória.")
        @Positive(message = "A distância deve ser maior que zero.")
        Double distanceKm,

        @NotNull(message = "O tipo de terreno é obrigatório.")
        TerrainType terrain,

        @NotNull(message = "A data da viagem é obrigatória.")
        LocalDate tripDate,

        @NotNull(message = "A motocicleta é obrigatória.")
        @Positive(message = "O identificador da motocicleta deve ser maior que zero.")
        Long motorcycleId

) {
}
