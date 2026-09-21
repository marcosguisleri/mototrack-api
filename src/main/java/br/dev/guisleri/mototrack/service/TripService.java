package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final Clock clock;

    public TripService(TripRepository tripRepository, Clock clock) {
        this.tripRepository = tripRepository;
        this.clock = clock;
    }

    public Trip scheduleTrip(
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrainType,
            LocalDate tripDate,
            Motorcycle motorcycle
    ) {
        Trip trip = Trip.schedule(
                origin,
                destination,
                distanceKm,
                terrainType,
                tripDate,
                motorcycle,
                clock
        );

        return tripRepository.save(trip);
    }

    public Trip registerCompletedTrip(
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrainType,
            LocalDate tripDate,
            Motorcycle motorcycle
    ) {
        Trip trip = Trip.registerCompleted(
                origin,
                destination,
                distanceKm,
                terrainType,
                tripDate,
                motorcycle,
                clock
        );

        return tripRepository.save(trip);
    }

    public void changeTripStatus(long tripId, TripStatus newStatus) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(
                        "Viagem com id %d não encontrada".formatted(tripId)
                ));

        trip.changeStatus(newStatus,
                LocalDate.now(clock));
        tripRepository.save(trip);
    }

    public Trip findTripById(long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(
                        "Viagem com id %d não encontrada".formatted(tripId)
                ));
    }

    public List<Trip> findAllTrips() {
        return tripRepository.findAll();
    }

    public List<Trip> findTripsByTerrain(TerrainType terrainType) {
        return tripRepository.findByTerrain(terrainType);
    }

    public List<Trip> findTripsByStatus(TripStatus status) {
        return tripRepository.findByStatus(status);
    }

    public List<Trip> findTripsByDate(LocalDate tripDate) {
        return tripRepository.findByTripDate(tripDate);
    }

    public List<Trip> findUpcomingTrips() {
        return tripRepository.findUpcomingFrom(LocalDate.now(clock));
    }

    public long calculateDaysUntilTrip(long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(
                        "Viagem com id %d não encontrada".formatted(tripId)
                ));

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new InvalidTripStatusException(
                    "Só é possível calcular os dias restantes de uma viagem planejada."
            );
        }

        return ChronoUnit.DAYS.between(
                LocalDate.now(clock),
                trip.getTripDate()
        );
    }

    public void deleteTripById(Long tripId) {
        findTripById(tripId);
        tripRepository.deleteById(tripId);
    }

}
