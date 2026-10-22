package br.dev.guisleri.mototrack.persistence.entity;

import br.dev.guisleri.mototrack.model.MotorcycleType;
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

@Entity
@Table(name = "motorcycles")
public class MotorcycleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String color;

    @Column(name = "manufacture_year", nullable = false)
    private int year;

    @Column(name = "engine_capacity", nullable = false)
    private int engineCapacity;

    @Enumerated(EnumType.STRING)
    @Column
    private MotorcycleType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity owner;

    protected MotorcycleEntity() {
    }

    public MotorcycleEntity(
            Long id,
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity,
            MotorcycleType type,
            UserEntity owner
    ) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.year = year;
        this.engineCapacity = engineCapacity;
        this.type = type;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getColor() {
        return color;
    }

    public int getYear() {
        return year;
    }

    public int getEngineCapacity() {
        return engineCapacity;
    }

    public MotorcycleType getType() {
        return type;
    }

    public UserEntity getOwner() {
        return owner;
    }
}
