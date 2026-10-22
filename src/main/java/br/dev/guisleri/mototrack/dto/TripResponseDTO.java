package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TripResponseDTO(
        long id,
        String origin,
        String destination,
        double distanceKm,
        TripStatus status,
        TerrainType terrain,
        LocalDate tripDate,
        MotorcycleResponseDTO motorcycle,
        Instant startedAt,
        Instant completedAt
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
                MotorcycleResponseDTO.from(trip.getMotorcycle()),
                trip.getStartedAt(),
                trip.getCompletedAt()
        );
    }
}
