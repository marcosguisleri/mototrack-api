package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.Motorcycle;

public record MotorcycleResponseDTO(
        Long id,
        String brand,
        String model,
        String color,
        int year,
        int engineCapacity
) {

    public static MotorcycleResponseDTO from(Motorcycle motorcycle) {
        return new MotorcycleResponseDTO(
                motorcycle.getId(),
                motorcycle.getBrand(),
                motorcycle.getModel(),
                motorcycle.getColor(),
                motorcycle.getYear(),
                motorcycle.getEngineCapacity()
        );
    }
}
