package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.MotorcycleResponseDTO;
import br.dev.guisleri.mototrack.dto.TripStatisticsResponseDTO;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.TripStatisticsService;
import br.dev.guisleri.mototrack.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        MotorcycleResponseDTO mostUsedMotorcycleResponse =
                tripStatisticsService.findMostUsedMotorcycleInCompletedTrips(ownerId)
                        .map(MotorcycleResponseDTO::from)
                        .orElse(null);

        TripStatisticsResponseDTO statisticsResponse =
                new TripStatisticsResponseDTO(
                        tripStatisticsService.countCompletedTrips(ownerId),
                        roundToOneDecimal(
                                tripStatisticsService.calculateTotalCompletedDistanceKm(ownerId)
                        ),
                        mostUsedMotorcycleResponse,
                        tripStatisticsService.countTripsByStatus(ownerId)
                );

        return ResponseEntity.ok(statisticsResponse);
    }

    private double roundToOneDecimal(double distanceKm) {
        return Math.round(distanceKm * 10.0) / 10.0;
    }

}
