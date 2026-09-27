package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.dto.TripStatisticsResponseDTO;
import br.dev.guisleri.mototrack.dto.TripSummaryResponseDTO;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.TripStatisticsService;
import br.dev.guisleri.mototrack.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/statistics")
public class TripStatisticsController {

    private final TripStatisticsService tripStatisticsService;
    private final UserService userService;

    public TripStatisticsController(
            TripStatisticsService tripStatisticsService,
            UserService userService
    ) {
        this.tripStatisticsService = tripStatisticsService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<TripStatisticsResponseDTO> getTripStatistics(
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());
        Long ownerId = currentUser.getId();


        // Resumo geral

        Long totalCompletedTrips =
                tripStatisticsService.countCompletedTrips(ownerId);

        Double totalCompletedDistance =
                tripStatisticsService.calculateTotalCompletedDistanceKm(ownerId);

        Double averageCompletedDistance =
                tripStatisticsService.calculateAverageCompletedDistanceKm(ownerId);


        // Destaques

        MotorcycleResponseDTO mostUsedMotorcycleResponse =
                tripStatisticsService.findMostUsedMotorcycleInCompletedTrips(ownerId)
                        .map(MotorcycleResponseDTO::from)
                        .orElse(null);

        TripSummaryResponseDTO longestTripResponse =
                tripStatisticsService.findLongestCompletedTrip(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);

        TripSummaryResponseDTO lastCompletedTripResponse =
                tripStatisticsService.findLastCompletedTrip(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);

        TripSummaryResponseDTO firstCompletedTripResponse =
                tripStatisticsService.findFirstCompletedTrip(ownerId)
                        .map(TripSummaryResponseDTO::from)
                        .orElse(null);

        // Distribuição

        Map<TripStatus, Long> tripsByStatus =
                tripStatisticsService.countTripsByStatus(ownerId);

        Map<TerrainType, TerrainStatistics> terrainStatistics =
                tripStatisticsService.findTerrainStatistics(ownerId);



        TripStatisticsResponseDTO statisticsResponse =
                new TripStatisticsResponseDTO(
                        totalCompletedTrips,
                        roundToOneDecimal(totalCompletedDistance),
                        averageCompletedDistance,
                        mostUsedMotorcycleResponse,
                        longestTripResponse,
                        firstCompletedTripResponse,
                        lastCompletedTripResponse,
                        tripsByStatus,
                        terrainStatistics
                );

        return ResponseEntity.ok(statisticsResponse);
    }

    private double roundToOneDecimal(double distanceKm) {
        return Math.round(distanceKm * 10.0) / 10.0;
    }
}