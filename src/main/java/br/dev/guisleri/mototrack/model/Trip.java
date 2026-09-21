package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;

import java.time.Clock;
import java.time.LocalDate;

public class Trip {

    private final Long id;
    private final String origin;
    private final String destination;
    private final double distanceKm;
    private TripStatus status;
    private final TerrainType terrain;
    private final LocalDate tripDate;
    private final Motorcycle motorcycle;

    private Trip(
            Long id,
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            TripStatus status
    ) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.terrain = terrain;
        this.tripDate = tripDate;
        this.motorcycle = motorcycle;
        this.status = status;
    }

    public static Trip schedule(
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            Clock clock
    ) {
        LocalDate currentDate = LocalDate.now(clock);

        if (tripDate.isBefore(currentDate)) {
            throw new InvalidTripDateException(
                    "Não é possível planejar uma viagem para uma data passada."
            );
        }

        return new Trip(
                null,
                origin,
                destination,
                distanceKm,
                terrain,
                tripDate,
                motorcycle,
                TripStatus.PLANNED
        );
    }

    public static Trip registerCompleted(
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            Clock clock
    ) {
        LocalDate currentDate = LocalDate.now(clock);

        if (tripDate.isAfter(currentDate)) {
            throw new InvalidTripDateException(
                    "Uma viagem concluída não pode ter data futura."
            );
        }

        return new Trip(
                null,
                origin,
                destination,
                distanceKm,
                terrain,
                tripDate,
                motorcycle,
                TripStatus.COMPLETED
        );
    }

    public static Trip restore(
            Long id,
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            TripStatus status
    ) {
        return new Trip(
                id,
                origin,
                destination,
                distanceKm,
                terrain,
                tripDate,
                motorcycle,
                status
        );
    }

    public Long getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void changeStatus(
            TripStatus newStatus,
            LocalDate currentDate
    ) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidTripStatusException("Status inválido!");
        }

        if (newStatus == TripStatus.COMPLETED
                && tripDate.isAfter(currentDate)) {
            throw new InvalidTripDateException(
                    "Uma viagem futura não pode ser concluída."
            );
        }

        status = newStatus;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public Motorcycle getMotorcycle() {
        return motorcycle;
    }

}
