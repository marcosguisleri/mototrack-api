package br.dev.guisleri.mototrack.dto;

public record HomeResponseDTO(
        TripSummaryResponseDTO inProgressTrip,
        NextTripResponseDTO nextTrip,
        TripSummaryResponseDTO lastCompletedTrip,
        double totalCompletedDistanceKm,
        double distanceThisYearKm,
        long completedTrips,
        long motorcycleCount
) {
}
