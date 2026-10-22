package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.FuelEntryRequestDTO;
import br.dev.guisleri.mototrack.dto.FuelEntryResponseDTO;
import br.dev.guisleri.mototrack.model.FuelEntry;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.FuelEntryService;
import br.dev.guisleri.mototrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trips/{tripId}/fuel-entries")
public class FuelEntryController {

    private final FuelEntryService fuelEntryService;
    private final UserService userService;

    public FuelEntryController(
            FuelEntryService fuelEntryService,
            UserService userService
    ) {
        this.fuelEntryService = fuelEntryService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<FuelEntryResponseDTO> registerFuelEntry(
            @PathVariable("tripId") long tripId,
            @Valid @RequestBody FuelEntryRequestDTO requestDTO,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        FuelEntry fuelEntry = fuelEntryService.registerFuelEntryForOwner(
                tripId,
                currentUser.getId(),
                requestDTO.liters(),
                requestDTO.totalCost()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(FuelEntryResponseDTO.from(fuelEntry));
    }

    @GetMapping
    public ResponseEntity<List<FuelEntryResponseDTO>> findFuelEntries(
            @PathVariable("tripId") long tripId,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        List<FuelEntryResponseDTO> fuelEntries = fuelEntryService
                .findFuelEntriesByTripIdForOwner(tripId, currentUser.getId())
                .stream()
                .map(FuelEntryResponseDTO::from)
                .toList();

        return ResponseEntity.ok(fuelEntries);
    }

    @PutMapping("/{fuelEntryId}")
    public ResponseEntity<FuelEntryResponseDTO> updateFuelEntry(
            @PathVariable("tripId") long tripId,
            @PathVariable("fuelEntryId") long fuelEntryId,
            @Valid @RequestBody FuelEntryRequestDTO requestDTO,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        FuelEntry fuelEntry = fuelEntryService.updateFuelEntryForOwner(
                tripId,
                fuelEntryId,
                currentUser.getId(),
                requestDTO.liters(),
                requestDTO.totalCost()
        );

        return ResponseEntity.ok(FuelEntryResponseDTO.from(fuelEntry));
    }

    @DeleteMapping("/{fuelEntryId}")
    public ResponseEntity<Void> deleteFuelEntry(
            @PathVariable("tripId") long tripId,
            @PathVariable("fuelEntryId") long fuelEntryId,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        fuelEntryService.deleteFuelEntryForOwner(
                tripId,
                fuelEntryId,
                currentUser.getId()
        );

        return ResponseEntity.noContent().build();
    }

}
