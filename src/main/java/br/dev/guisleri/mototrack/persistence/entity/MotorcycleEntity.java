package br.dev.guisleri.mototrack.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "motorcycles")
public class MotorcycleEntity {

    @Id
    private long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "manufacture_year", nullable = false)
    private int year;

    @Column(name = "engine_capacity", nullable = false)
    private int engineCapacity;

    protected MotorcycleEntity() {
    }

    public MotorcycleEntity(
            long id,
            String brand,
            String model,
            int year,
            int engineCapacity
    ) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.engineCapacity = engineCapacity;
    }

    public long getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public int getEngineCapacity() {
        return engineCapacity;
    }
}
