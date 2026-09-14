package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.TripRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class TripService {

    private final TripRepository repository;
    private final Clock clock;

    public TripService(TripRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void registerTrip(Trip trip) {
        repository.save(trip);
    }

    public Optional<Trip> findTripById(long id) {
        return repository.findById(id);
    }

    public List<Trip> findAllTrips() {
        return repository.findAll();
    }

    public void changeTripStatus(long id, TripStatus newStatus) {
        Trip trip = repository.findById(id)
                .orElseThrow(() -> new TripNotFoundException(
                        "Viagem com id %d não encontrada".formatted(id)
                ));

        trip.changeStatus(newStatus,
                LocalDate.now(clock));
        repository.save(trip);
    }

    public List<Trip> findTripsByTerrain(TerrainType terrainType) {
        return repository.findByTerrain(terrainType);
    }

    public List<Trip> findPlannedTrips() {
        return repository.findByStatus(TripStatus.PLANNED);
    }

    public List<Trip> findTripsByDate(LocalDate date) {
        return repository.findByTripDate(date);
    }

    public List<Trip> findUpcomingTrips() {
        return repository.findUpcomingFrom(LocalDate.now(clock));
    }

    public long calculateDaysUntilTrip(long id) {
        Trip trip = repository.findById(id)
                .orElseThrow(() -> new TripNotFoundException(
                        "Viagem com id %d não encontrada".formatted(id)
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

}
