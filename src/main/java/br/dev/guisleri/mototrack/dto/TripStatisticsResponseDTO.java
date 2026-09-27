package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.MonthlyTripStatistics;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;

import java.util.List;
import java.util.Map;

public record TripStatisticsResponseDTO(
        long totalCompletedTrips,
        double totalCompletedDistance,
        Double averageCompletedDistanceKm,
        MotorcycleResponseDTO mostUsedMotorcycle,
        TripSummaryResponseDTO longestTrip,
        TripSummaryResponseDTO firstCompletedTrip,
        TripSummaryResponseDTO lastCompletedTrip,
        Map<TripStatus, Long> tripsByStatus,
        Map<TerrainType, TerrainStatistics> terrainStatistics,
        List<MonthlyTripStatistics> monthlyStatistics
) {
}
