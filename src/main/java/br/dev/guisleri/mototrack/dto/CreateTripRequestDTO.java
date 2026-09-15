package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;

import java.time.LocalDate;

public record CreateTripRequestDTO(
        long id,
        String origin,
        String destination,
        double distanceKm,
        TerrainType terrain,
        LocalDate tripDate,
        Motorcycle motorcycle
) {
}
