package br.dev.guisleri.mototrack.model;

public class Motorcycle {

    private final long id;
    private String brand;
    private String model;
    private int year;
    private int engineCapacity;

    public Motorcycle(long id, String brand, String model, int year, int engineCapacity) {
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
