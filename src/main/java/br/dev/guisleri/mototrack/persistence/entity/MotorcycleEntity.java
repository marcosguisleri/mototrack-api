package br.dev.guisleri.mototrack.persistence.entity;

import jakarta.persistence.*;

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
            UserEntity owner
    ) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.year = year;
        this.engineCapacity = engineCapacity;
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

    public UserEntity getOwner() {
        return owner;
    }
}
