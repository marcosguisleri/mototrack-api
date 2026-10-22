package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.TripCostSummaryResponseDTO;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.TripCostSummaryService;
import br.dev.guisleri.mototrack.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips/{tripId}/cost-summary")
public class TripCostSummaryController {

    private final TripCostSummaryService tripCostSummaryService;
    private final UserService userService;

    public TripCostSummaryController(
            TripCostSummaryService tripCostSummaryService,
            UserService userService
    ) {
        this.tripCostSummaryService = tripCostSummaryService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<TripCostSummaryResponseDTO> getCostSummary(
            @PathVariable("tripId") long tripId,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        return ResponseEntity.ok(
                TripCostSummaryResponseDTO.from(
                        tripCostSummaryService.getCostSummaryForOwner(
                                tripId,
                                currentUser.getId()
                        )
                )
        );
    }

}
