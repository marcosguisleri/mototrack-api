package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.ChangeTripStatusRequestDTO;
import br.dev.guisleri.mototrack.dto.CreateTripRequestDTO;
import br.dev.guisleri.mototrack.dto.TripResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import br.dev.guisleri.mototrack.service.TripService;
import br.dev.guisleri.mototrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final MotorcycleService motorcycleService;
    private final UserService userService;

    public TripController(
            TripService tripService,
            MotorcycleService motorcycleService,
            UserService userService
    ) {
        this.tripService = tripService;
        this.motorcycleService = motorcycleService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<TripResponseDTO> scheduleTrip(
            @Valid @RequestBody CreateTripRequestDTO createTripRequest,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User currentUser = userService.findUserByEmail(email);

        Motorcycle motorcycle = motorcycleService
                .findMotorcycleByIdForOwner(
                        createTripRequest.motorcycleId(),
                        currentUser.getId()
                );

        Trip trip = tripService.scheduleTrip(
                createTripRequest.origin(),
                createTripRequest.destination(),
                createTripRequest.distanceKm(),
                createTripRequest.terrain(),
                createTripRequest.tripDate(),
                motorcycle
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TripResponseDTO.from(trip));
    }

    @PostMapping("/completed")
    public ResponseEntity<TripResponseDTO> registerCompletedTrip(
            @Valid @RequestBody CreateTripRequestDTO createTripRequest,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User currentUser = userService.findUserByEmail(email);

        Motorcycle motorcycle = motorcycleService
                .findMotorcycleByIdForOwner(
                        createTripRequest.motorcycleId(),
                        currentUser.getId()
                );

        Trip completedTrip = tripService.registerCompletedTrip(
                createTripRequest.origin(),
                createTripRequest.destination(),
                createTripRequest.distanceKm(),
                createTripRequest.terrain(),
                createTripRequest.tripDate(),
                motorcycle
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TripResponseDTO.from(completedTrip));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeTripStatus(
            @PathVariable("id") long tripId,
            @Valid @RequestBody ChangeTripStatusRequestDTO statusChangeRequest,
            Authentication authentication
    ) {

        User currentUser = userService.findUserByEmail(authentication.getName());

        tripService.changeTripStatusForOwner(
                tripId,
                currentUser.getId(),
                statusChangeRequest.status()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTripById(
            @PathVariable("id") Long tripId,
            Authentication authentication
    ) {

        User currentUser = userService.findUserByEmail(authentication.getName());

        tripService.deleteTripByIdForOwner(tripId, currentUser.getId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<TripResponseDTO>> findTrips(
            @RequestParam(required = false) TripStatus status,
            Authentication authentication
    ) {
        List<Trip> trips;

        String email = authentication.getName();

        User currentUser = userService.findUserByEmail(email);

        if (status == null) {
            trips = tripService.findTripsByOwnerId(currentUser.getId());
        } else {
            trips = tripService.findTripsByOwnerIdAndStatus(currentUser.getId(), status);
        }

        List<TripResponseDTO> tripResponses = trips.stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(tripResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDTO> findTripById(
            @PathVariable("id") long tripId,
            Authentication authentication
    ) {

        User currentUser = userService.findUserByEmail(authentication.getName());

        Trip trip = tripService.findTripByIdForOwner(tripId, currentUser.getId());

        return ResponseEntity.ok(TripResponseDTO.from(trip));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<TripResponseDTO>> findUpcomingTrips(
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(
                authentication.getName()
        );

        List<TripResponseDTO> tripResponses =
                tripService.findUpcomingTripsByOwnerId(currentUser.getId())
                        .stream()
                        .map(TripResponseDTO::from)
                        .toList();

        return ResponseEntity.ok(tripResponses);
    }

    @GetMapping("/terrain/{terrain}")
    public ResponseEntity<List<TripResponseDTO>> findTripsByTerrain(
            @PathVariable("terrain") TerrainType terrainType,
            Authentication authentication
    ) {

        User currentUser = userService.findUserByEmail(authentication.getName());

        List<TripResponseDTO> tripResponses =
                tripService.findTripsByOwnerIdAndTerrain(
                                currentUser.getId(),
                                terrainType)
                        .stream()
                        .map(TripResponseDTO::from)
                        .toList();

        return ResponseEntity.ok(tripResponses);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<TripResponseDTO>> findTripsByDate(
            @PathVariable("date") LocalDate tripDate,
            Authentication authentication
    ) {

        User currentUser = userService.findUserByEmail(authentication.getName());

        List<TripResponseDTO> tripResponses = tripService
                .findTripsByOwnerIdAndDate(
                        currentUser.getId(),
                        tripDate
                ).stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(tripResponses);
    }

    @GetMapping("/{id}/days-until")
    public ResponseEntity<Long> calculateDaysUntilTrip(
            @PathVariable("id") long tripId,
            Authentication authentication
    ) {
        User currentUser = userService.findUserByEmail(authentication.getName());

        return ResponseEntity.ok(
                tripService.calculateDaysUntilTripForOwner(
                        tripId,
                        currentUser.getId()
                )
        );
    }
}
