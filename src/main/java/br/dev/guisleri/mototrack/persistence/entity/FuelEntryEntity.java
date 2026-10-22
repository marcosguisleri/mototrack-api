package br.dev.guisleri.mototrack.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fuel_entries")
public class FuelEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @Column(nullable = false, precision = 7, scale = 3)
    private BigDecimal liters;

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FuelEntryEntity() {
    }

    public FuelEntryEntity(
            Long id,
            TripEntity trip,
            BigDecimal liters,
            BigDecimal totalCost,
            Instant createdAt
    ) {
        this.id = id;
        this.trip = trip;
        this.liters = liters;
        this.totalCost = totalCost;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public TripEntity getTrip() {
        return trip;
    }

    public BigDecimal getLiters() {
        return liters;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
