package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;

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
                .orElseThrow(() ->
                        new TripNotFoundException(
                                "Viagem com id %d não encontrada".formatted(id)
                        )
                );

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
}
