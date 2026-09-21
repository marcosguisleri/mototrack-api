package br.dev.guisleri.mototrack.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class Motorcycle {

    private final Long id;

    @NotBlank
    private final String brand;

    @NotBlank
    private final String model;

    @NotBlank
    private final String color;

    @Min(1900)
    private final int year;

    @Positive
    private final int engineCapacity;

    private Motorcycle(
            Long id,
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity
    ) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.year = year;
        this.engineCapacity = engineCapacity;
    }

    public static Motorcycle register(
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity
    ) {
        return new Motorcycle(
                null,
                brand,
                model,
                color,
                year,
                engineCapacity
        );
    }

    public static Motorcycle restore(
            Long id,
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity
    ) {
        return new Motorcycle(
                id,
                brand,
                model,
                color,
                year,
                engineCapacity
        );
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Motorcycle motorcycle = (Motorcycle) object;

        return id != null && id.equals(motorcycle.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
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

}
