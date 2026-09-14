package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.TripRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TripService {

    private final TripRepository repository;

    public TripService(TripRepository repository) {
        this.repository = repository;
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

        trip.changeStatus(newStatus);
        repository.save(trip);
    }

    public List<Trip> findTripsByTerrain(TerrainType terrainType) {
        return repository.findByTerrain(terrainType);
    }

    public List<Trip> findPlannedTrips() {
        return repository.findByStatus(TripStatus.PLANNED);
    }

    public Map<TripStatus, Long> countTripsByStatus() {
        return repository.countByStatus();
    }

    public List<Trip> findTripsByDate(LocalDate date) {
        return repository.findByPlannedDate(date);
    }

    public List<Trip> findUpcomingTrips() {
        return repository.findUpcomingFrom(LocalDate.now());
    }

    public long countCompletedTrips() {
        return repository.countByStatus()
                .getOrDefault(TripStatus.COMPLETED, 0L);
    }

    public double calculateTotalCompletedDistance() {
        return repository.sumDistanceByStatus(TripStatus.COMPLETED);
    }

    public Map<Motorcycle, Long> countTripsByMotorcycle() {
        return repository.countByMotorcycle();
    }

    public double calculateCompletedDistanceByMotorcycle(
            Motorcycle motorcycle
    ) {
        return repository.sumDistanceByMotorcycleAndStatus(
                motorcycle,
                TripStatus.COMPLETED
        );
    }

}
