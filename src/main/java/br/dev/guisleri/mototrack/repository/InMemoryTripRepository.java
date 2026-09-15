package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class InMemoryTripRepository implements TripRepository {

    private final List<Trip> trips = new ArrayList<>();

    @Override
    public void save(Trip trip) {
        for (int index = 0; index < trips.size(); index++) {
            if (trips.get(index).getId() == trip.getId()) {
                trips.set(index, trip);
                return;
            }
        }

        trips.add(trip);
    }

    @Override
    public Optional<Trip> findById(long id) {
        return trips.stream()
                .filter(trip -> trip.getId() == id)
                .findFirst();
    }

    @Override
    public List<Trip> findAll() {
        return List.copyOf(trips);
    }

    @Override
    public List<Trip> findByTerrain(TerrainType terrainType) {
        return trips.stream()
                .filter(trip -> trip.getTerrain() == terrainType)
                .toList();
    }

    @Override
    public List<Trip> findByStatus(TripStatus status) {
        return trips.stream()
                .filter(trip -> trip.getStatus() == status)
                .toList();
    }

    @Override
    public Map<TripStatus, Long> countByStatus() {
        return trips.stream()
                .collect(Collectors.groupingBy(
                        Trip::getStatus,
                        Collectors.counting()
                ));
    }

    @Override
    public List<Trip> findByTripDate(LocalDate date) {
        return trips.stream()
                .filter(trip -> trip.getTripDate().isEqual(date))
                .toList();
    }

    @Override
    public List<Trip> findUpcomingFrom(LocalDate date) {
        return trips.stream()
                .filter(trip -> !trip.getTripDate().isBefore(date))
                .filter(trip -> trip.getStatus() == TripStatus.PLANNED)
                .sorted(Comparator.comparing(Trip::getTripDate))
                .toList();
    }

    @Override
    public double sumDistanceByStatus(TripStatus status) {
        return trips.stream()
                .filter(trip -> trip.getStatus() == status)
                .mapToDouble(Trip::getDistanceKm)
                .sum();
    }

    @Override
    public Map<Motorcycle, Long> countByMotorcycle() {
        return trips.stream()
                .collect(Collectors.groupingBy(
                        Trip::getMotorcycle,
                        Collectors.counting()
                ));
    }

    @Override
    public double sumDistanceByMotorcycleAndStatus(
            Motorcycle motorcycle,
            TripStatus status
    ) {
        return trips.stream()
                .filter(trip -> Objects.equals(trip.getMotorcycle(), motorcycle))
                .filter(trip -> trip.getStatus() == status)
                .mapToDouble(Trip::getDistanceKm)
                .sum();
    }

    @Override
    public Optional<Motorcycle> findMostUsedMotorcycle() {
        return findAll().stream()
                .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        Trip::getMotorcycle,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

}
