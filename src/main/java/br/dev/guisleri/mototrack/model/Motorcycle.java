package br.dev.guisleri.mototrack.model;

public class Motorcycle {

    private final long id;
    private final String brand;
    private final String model;
    private final int year;
    private final int engineCapacity;

    public Motorcycle(long id, String brand, String model, int year, int engineCapacity) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.engineCapacity = engineCapacity;
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

        return id == motorcycle.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
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
