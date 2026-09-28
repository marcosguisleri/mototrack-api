package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;

import java.time.LocalDate;

public record MotorcycleTripSummaryResponseDTO(
        long id,
        String origin,
        String destination,
        double distanceKm,
        TerrainType terrain,
        LocalDate tripDate
) {
    public static MotorcycleTripSummaryResponseDTO from(Trip trip) {
        return new MotorcycleTripSummaryResponseDTO(
                trip.getId(),
                trip.getOrigin(),
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getTerrain(),
                trip.getTripDate()
        );
    }
}
