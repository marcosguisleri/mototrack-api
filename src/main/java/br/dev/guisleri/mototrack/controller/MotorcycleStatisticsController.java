package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.MotorcycleStatisticsResponseDTO;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.MotorcycleStatisticsService;
import br.dev.guisleri.mototrack.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/motorcycles")
public class MotorcycleStatisticsController {

    private final MotorcycleStatisticsService motorcycleStatisticsService;
    private final UserService userService;

    public MotorcycleStatisticsController(
            MotorcycleStatisticsService motorcycleStatisticsService,
            UserService userService
    ) {
        this.motorcycleStatisticsService = motorcycleStatisticsService;
        this.userService = userService;
    }

    @GetMapping("/{motorcycleId}/statistics")
    public ResponseEntity<MotorcycleStatisticsResponseDTO> getMotorcycleStatistics(
            @PathVariable("motorcycleId") Long motorcycleId,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(
                authentication.getName()
        );

        MotorcycleStatisticsResponseDTO statistics =
                motorcycleStatisticsService.getStatistics(
                        currentUser.getId(),
                        motorcycleId
                );

        return ResponseEntity.ok(statistics);
    }
}
