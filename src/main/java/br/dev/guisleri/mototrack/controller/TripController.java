package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.dto.ChangeTripStatusRequestDTO;
import br.dev.guisleri.mototrack.dto.CreateTripRequestDTO;
import br.dev.guisleri.mototrack.dto.TripResponseDTO;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<TripResponseDTO> scheduleTrip(
            @RequestBody CreateTripRequestDTO request
    ) {
        Trip trip = tripService.scheduleTrip(
                request.id(),
                request.origin(),
                request.destination(),
                request.distanceKm(),
                request.terrain(),
                request.tripDate(),
                request.motorcycle()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TripResponseDTO.from(trip));
    }

    @PostMapping("/completed")
    public ResponseEntity<TripResponseDTO> registerCompletedTrip(
            @RequestBody CreateTripRequestDTO request
    ) {
        Trip completedTrip = tripService.registerCompletedTrip(
                request.id(),
                request.origin(),
                request.destination(),
                request.distanceKm(),
                request.terrain(),
                request.tripDate(),
                request.motorcycle()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TripResponseDTO.from(completedTrip));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable long id,
            @RequestBody ChangeTripStatusRequestDTO request
    ) {
        tripService.changeTripStatus(
                id,
                request.status()
        );

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

        List<TripResponseDTO> response = trips.stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDTO> findTripById(@PathVariable long id) {
        Trip tripById = tripService.findTripById(id);

        return ResponseEntity.ok(TripResponseDTO.from(tripById));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<TripResponseDTO>> findUpcomingTrips() {
        List<TripResponseDTO> responseDTOs = tripService.findUpcomingTrips()
                .stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/terrain/{terrain}")
    public ResponseEntity<List<TripResponseDTO>> findAllByTerrain(@PathVariable TerrainType terrain) {
        List<TripResponseDTO> responseDTOs = tripService.findTripsByTerrain(terrain)
                .stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<TripResponseDTO>> findAllByDate(@PathVariable LocalDate date) {
        List<TripResponseDTO> responseDTOs = tripService.findTripsByDate(date)
                .stream()
                .map(TripResponseDTO::from)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}/days-until")
    public ResponseEntity<Long> calculateDaysUntilTrip(@PathVariable long id) {
        return ResponseEntity.ok(
                tripService.calculateDaysUntilTrip(id)
        );
    }
}
