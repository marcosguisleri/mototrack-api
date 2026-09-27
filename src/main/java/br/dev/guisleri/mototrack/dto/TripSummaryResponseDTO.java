package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;

import java.time.LocalDate;

public record TripSummaryResponseDTO(
        long id,
        String origin,
        String destination,
        double distanceKm,
        TerrainType terrain,
        LocalDate tripDate,
        MotorcycleResponseDTO motorcycle
) {
    public static TripSummaryResponseDTO from(Trip trip) {
        return new TripSummaryResponseDTO(
                trip.getId(),
                trip.getOrigin(),
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getTerrain(),
                trip.getTripDate(),
                MotorcycleResponseDTO.from(trip.getMotorcycle())
        );
    }
}
