package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;

import java.time.LocalDate;

public record TripResponseDTO(
        long id,
        String origin,
        String destination,
        double distanceKm,
        TripStatus status,
        TerrainType terrain,
        LocalDate tripDate,
        Motorcycle motorcycle
) {
    public static TripResponseDTO from(Trip trip) {
        return new TripResponseDTO(
                trip.getId(),
                trip.getOrigin(),
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getStatus(),
                trip.getTerrain(),
                trip.getTripDate(),
                trip.getMotorcycle()
        );
    }
}
