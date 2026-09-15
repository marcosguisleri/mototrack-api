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
    public ResponseEntity<TripStatisticsResponseDTO> getStatistics() {

        MotorcycleResponseDTO mostUsedMotorcycle =
                tripStatisticsService.getMostUsedMotorcycleInCompletedTrips()
                        .map(MotorcycleResponseDTO::from)
                        .orElse(null);

        TripStatisticsResponseDTO responseDTO =
                new TripStatisticsResponseDTO(
                        tripStatisticsService.countCompletedTrips(),
                        roundToOneDecimal(
                                tripStatisticsService.calculateTotalCompletedDistance()
                        ),
                        mostUsedMotorcycle,
                        tripStatisticsService.countTripsByStatus()
                );

        return ResponseEntity.ok(responseDTO);
    }

    private double roundToOneDecimal(double distance) {
        return Math.round(distance * 10.0) / 10.0;
    }

}
