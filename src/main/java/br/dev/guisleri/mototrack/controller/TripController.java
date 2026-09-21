package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.ChangeTripStatusRequestDTO;
import br.dev.guisleri.mototrack.dto.CreateTripRequestDTO;
import br.dev.guisleri.mototrack.dto.TripResponseDTO;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import br.dev.guisleri.mototrack.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final MotorcycleService motorcycleService;

    public TripController(
            TripService tripService,
            MotorcycleService motorcycleService
    ) {
        this.tripService = tripService;
        this.motorcycleService = motorcycleService;
    }

    @PostMapping
    public ResponseEntity<TripResponseDTO> scheduleTrip(
            @Valid @RequestBody CreateTripRequestDTO createTripRequest
    ) {

        Motorcycle motorcycle = motorcycleService.findMotorcycleById(
                createTripRequest.motorcycleId()
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
            @Valid @RequestBody CreateTripRequestDTO createTripRequest
    ) {

        Motorcycle motorcycle = motorcycleService.findMotorcycleById(
                createTripRequest.motorcycleId()
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
            @Valid @RequestBody ChangeTripStatusRequestDTO statusChangeRequest
    ) {
        tripService.changeTripStatus(
                tripId,
                statusChangeRequest.status()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTripById(
            @PathVariable("id") Long tripId
    ) {
        tripService.deleteTripById(tripId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<TripResponseDTO>> findTrips(
            @RequestParam(required = false) TripStatus status
    ) {
        List<Trip> trips;

        if (status == null) {
            trips = tripService.findAllTrips();
        } else {
            trips = tripService.findTripsByStatus(status);
        }

        List<TripResponseDTO> tripResponses = trips.stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(tripResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDTO> findTripById(
            @PathVariable("id") long tripId
    ) {
        Trip trip = tripService.findTripById(tripId);

        return ResponseEntity.ok(TripResponseDTO.from(trip));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<TripResponseDTO>> findUpcomingTrips() {
        List<TripResponseDTO> tripResponses = tripService.findUpcomingTrips()
                .stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(tripResponses);
    }

    @GetMapping("/terrain/{terrain}")
    public ResponseEntity<List<TripResponseDTO>> findTripsByTerrain(
            @PathVariable("terrain") TerrainType terrainType
    ) {
        List<TripResponseDTO> tripResponses = tripService.findTripsByTerrain(terrainType)
                .stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(tripResponses);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<TripResponseDTO>> findTripsByDate(
            @PathVariable("date") LocalDate tripDate
    ) {
        List<TripResponseDTO> tripResponses = tripService.findTripsByDate(tripDate)
                .stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(tripResponses);
    }

    @GetMapping("/{id}/days-until")
    public ResponseEntity<Long> calculateDaysUntilTrip(
            @PathVariable("id") long tripId
    ) {
        return ResponseEntity.ok(
                tripService.calculateDaysUntilTrip(tripId)
        );
    }
}
