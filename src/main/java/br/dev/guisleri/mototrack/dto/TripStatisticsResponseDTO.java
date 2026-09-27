package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;

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
        Map<TerrainType, TerrainStatistics> terrainStatistics
) {
}