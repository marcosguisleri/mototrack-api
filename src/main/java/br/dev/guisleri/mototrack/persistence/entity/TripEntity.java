package br.dev.guisleri.mototrack.persistence.entity;

import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trips")
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TerrainType terrain;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorcycle_id", nullable = false)
    private MotorcycleEntity motorcycle;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected TripEntity() {
    }

    public TripEntity(
            Long id,
            String origin,
            String destination,
            double distanceKm,
            TripStatus status,
            TerrainType terrain,
            LocalDate tripDate,
            MotorcycleEntity motorcycle,
            Instant startedAt,
            Instant completedAt
    ) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.status = status;
        this.terrain = terrain;
        this.tripDate = tripDate;
        this.motorcycle = motorcycle;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
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

    public TerrainType getTerrain() {
        return terrain;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public MotorcycleEntity getMotorcycle() {
        return motorcycle;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
