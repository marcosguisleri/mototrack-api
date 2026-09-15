package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TripStatus;

import java.util.Map;

public record TripStatisticsResponseDTO(
        long totalCompletedTrips,
        double totalCompletedDistance,
        MotorcycleResponseDTO mostUsedMotorcycle,
        Map<TripStatus, Long> tripsByStatus
) {
}
