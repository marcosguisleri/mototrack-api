package br.dev.guisleri.mototrack.model;

public class Motorcycle {

    private final Long id;
    private final String brand;
    private final String model;
    private final String color;
    private final int year;
    private final int engineCapacity;
    private final MotorcycleType type;
    private final User owner;

    private Motorcycle(
            Long id,
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity,
            MotorcycleType type,
            User owner
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

    public static Motorcycle register(
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity,
            MotorcycleType type,
            User owner
    ) {
        return new Motorcycle(
                null,
                brand,
                model,
                color,
                year,
                engineCapacity,
                type,
                owner
        );
    }

    public static Motorcycle restore(
            Long id,
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity,
            MotorcycleType type,
            User owner
    ) {
        return new Motorcycle(
                id,
                brand,
                model,
                color,
                year,
                engineCapacity,
                type,
                owner
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

    /**
     * Pode ser {@code null} em motos cadastradas antes de o tipo existir.
     */
    public MotorcycleType getType() {
        return type;
    }

    public User getOwner() {
        return owner;
    }
}
