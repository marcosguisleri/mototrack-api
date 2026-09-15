package br.dev.guisleri.mototrack.dto;

import br.dev.guisleri.mototrack.model.Motorcycle;

public record MotorcycleResponseDTO(
        long id,
        String brand,
        String model
) {

    public static MotorcycleResponseDTO from(Motorcycle motorcycle) {
        return new MotorcycleResponseDTO(
                motorcycle.getId(),
                motorcycle.getBrand(),
                motorcycle.getModel()
        );
    }
}
