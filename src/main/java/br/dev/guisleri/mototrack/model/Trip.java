package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Trip {

    private final long id;
    private final String origin;
    private String destination;
    private double distanceKm;
    private TripStatus status;
    private TerrainType terrain;
    private final LocalDate plannedDate;
    private final Motorcycle motorcycle;

    public Trip(long id,String origin, String destination, double distanceKm, TerrainType terrain, LocalDate plannedDate, Motorcycle motorcycle) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.status = TripStatus.PLANNED;
        this.terrain = terrain;

        if (plannedDate.isBefore(LocalDate.now())) {
            throw new InvalidTripDateException("Data planejada inválida!");
        }

        this.plannedDate = plannedDate;

        this.motorcycle = motorcycle;
    }

    public long getDaysUntilPlannedDate() {
        return ChronoUnit.DAYS.between(LocalDate.now(), this.plannedDate);
    }

    public long getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void changeStatus(TripStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidTripStatusException("Status inválido!");
        }
        this.status = newStatus;
    }


    public TerrainType getTerrain() {
        return terrain;
    }

    public void setTerrain(TerrainType terrain) {
        this.terrain = terrain;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public Motorcycle getMotorcycle() {
        return motorcycle;
    }

}
