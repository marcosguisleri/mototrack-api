package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;

import java.util.Map;

public record MotorcycleStatisticsResponseDTO(
        MotorcycleResponseDTO motorcycle,
        long completedTrips,
        double totalCompletedDistanceKm,
        Double averageCompletedDistanceKm,
        MotorcycleTripSummaryResponseDTO longestTrip,
        MotorcycleTripSummaryResponseDTO lastCompletedTrip,
        Map<TerrainType, TerrainStatistics> terrainStatistics
) {
}
