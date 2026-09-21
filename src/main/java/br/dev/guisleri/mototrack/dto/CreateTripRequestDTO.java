package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TerrainType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateTripRequestDTO(

        @NotBlank
        String origin,

        @NotBlank
        String destination,

        @NotNull
        @Positive
        Double distanceKm,

        @NotNull
        TerrainType terrain,

        @NotNull
        LocalDate tripDate,

        @NotNull
        @Positive
        Long motorcycleId
) {
}
