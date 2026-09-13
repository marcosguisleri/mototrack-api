package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TripService {

    private final List<Trip> trips = new ArrayList<>();

    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    public Optional<Trip> findTripById(long id) {
        return trips.stream()
                .filter(trip -> trip.getId() == id)
                .findFirst();
    }

    public void changeStatus(long id, TripStatus status) {
        Trip trip = findTripById(id)
                .orElseThrow(() -> new TripNotFoundException(
                        "Viagem com id %d não encontrada".formatted(id)
                ));

        trip.changeStatus(status);
    }


    public List<Trip> findTripsByTerrain(TerrainType terrainType) {
        return trips.stream()
                .filter(trip -> trip.getTerrain() == terrainType)
                .toList();
    }

    public List<Trip> findPlannedTrips() {
        return trips.stream()
                .filter(trip -> trip.getStatus() == TripStatus.PLANNED)
                .toList();
    }

    public Map<TripStatus, Long> countByStatus() {
        return trips.stream()
                .collect(Collectors.groupingBy(
                        Trip::getStatus,
                        Collectors.counting()
                ));
    }

    public List<Trip> findTripsByDate(LocalDate date) {
        return trips.stream()
                .filter(trip -> trip.getPlannedDate().isEqual(date))
                .toList();
    }

    public List<Trip> findUpcomingTrips() {
        return trips.stream()
                .filter(trip -> !trip.getPlannedDate().isBefore(LocalDate.now()))
                .filter(trip -> trip.getStatus() != TripStatus.COMPLETED)
                .sorted(Comparator.comparing(Trip::getPlannedDate))
                .toList();
    }

    public long getCompletedTripsCount() {
        return trips.stream()
                .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
                .count();
    }

    public double getTotalCompletedDistance() {
        return trips.stream()
                .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
                .mapToDouble(Trip::getDistanceKm)
                .sum();
    }

    public Map<Motorcycle, Long> countTripsByMotorcycle() {
        return trips.stream()
                .collect(Collectors.groupingBy(
                        Trip::getMotorcycle,
                        Collectors.counting()
                ));
    }

}
