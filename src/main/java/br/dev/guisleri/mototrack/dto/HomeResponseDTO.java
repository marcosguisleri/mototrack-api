package br.dev.guisleri.mototrack.dto;

public record HomeResponseDTO(
        NextTripResponseDTO nextTrip,
        TripSummaryResponseDTO lastCompletedTrip,
        double totalCompletedDistanceKm,
        long completedTrips,
        long motorcycleCount
) {
}
