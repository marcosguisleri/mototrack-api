package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.dto.TripStatisticsResponseDTO;
import br.dev.guisleri.mototrack.service.TripStatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
public class TripStatisticsController {

    private final TripStatisticsService tripStatisticsService;

    public TripStatisticsController(TripStatisticsService tripStatisticsService) {
        this.tripStatisticsService = tripStatisticsService;
    }

    @GetMapping
    public ResponseEntity<TripStatisticsResponseDTO> getTripStatistics() {

        MotorcycleResponseDTO mostUsedMotorcycleResponse =
                tripStatisticsService.findMostUsedMotorcycleInCompletedTrips()
                        .map(MotorcycleResponseDTO::from)
                        .orElse(null);

        TripStatisticsResponseDTO statisticsResponse =
                new TripStatisticsResponseDTO(
                        tripStatisticsService.countCompletedTrips(),
                        roundToOneDecimal(
                                tripStatisticsService.calculateTotalCompletedDistanceKm()
                        ),
                        mostUsedMotorcycleResponse,
                        tripStatisticsService.countTripsByStatus()
                );

        return ResponseEntity.ok(statisticsResponse);
    }

    private double roundToOneDecimal(double distanceKm) {
        return Math.round(distanceKm * 10.0) / 10.0;
    }

}
