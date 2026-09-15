package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementação simples do contrato de persistência para testes unitários de service.
 */
public class InMemoryTripRepository implements TripRepository {

    private final List<Trip> trips = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Trip save(Trip trip) {
        Trip tripToSave = trip;

        if (trip.getId() == null) {
            tripToSave = Trip.restore(
                    nextId++,
                    trip.getOrigin(),
                    trip.getDestination(),
                    trip.getDistanceKm(),
                    trip.getTerrain(),
                    trip.getTripDate(),
                    trip.getMotorcycle(),
                    trip.getStatus()
            );
        } else {
            nextId = Math.max(nextId, trip.getId() + 1);
        }

        for (int index = 0; index < trips.size(); index++) {
            if (Objects.equals(trips.get(index).getId(), tripToSave.getId())) {
                trips.set(index, tripToSave);
                return tripToSave;
            }
        }

        trips.add(tripToSave);
        return tripToSave;
    }

    @Override
    public Optional<Trip> findById(long id) {
        return trips.stream()
                .filter(trip -> Objects.equals(trip.getId(), id))
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
        Map<TripStatus, Long> counts = new EnumMap<>(TripStatus.class);

        for (TripStatus status : TripStatus.values()) {
            counts.put(status, 0L);
        }

        trips.forEach(trip -> counts.compute(
                trip.getStatus(),
                (status, count) -> count + 1
        ));

        return counts;
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
    public Optional<Motorcycle> findMostUsedMotorcycleInCompletedTrips() {
        return trips.stream()
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
