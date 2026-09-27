package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;

import java.time.LocalDate;

public record NextTripResponseDTO(
        long id,
        String origin,
        String destination,
        double distanceKm,
        TerrainType terrain,
        LocalDate tripDate,
        long daysUntil,
        MotorcycleResponseDTO motorcycle
) {
    public static NextTripResponseDTO from(Trip trip, long daysUntil) {
        return new NextTripResponseDTO(
                trip.getId(),
                trip.getOrigin(),
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getTerrain(),
                trip.getTripDate(),
                daysUntil,
                MotorcycleResponseDTO.from(trip.getMotorcycle())
        );
    }
}
