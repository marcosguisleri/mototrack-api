package br.dev.guisleri.mototrack.model;

public class Trip {

    private final long id;
    private String destination;
    private double distanceKm;
    private TripStatus status;
    private TerrainType terrain;

    public Trip(long id, String destination, double distanceKm, TerrainType terrain) {
        this.id = id;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.status = TripStatus.PLANNED;
        this.terrain = terrain;
    }

    public long getId() {
        return id;
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

    public void changeStatus(TripStatus status) {
        this.status = status;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public void setTerrain(TerrainType terrain) {
        this.terrain = terrain;
    }
}
