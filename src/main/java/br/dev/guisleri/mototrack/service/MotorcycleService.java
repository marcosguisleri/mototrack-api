package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.MotorcycleInUseException;
import br.dev.guisleri.mototrack.exception.MotorcycleNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.repository.MotorcycleRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MotorcycleService {

    private final MotorcycleRepository motorcycleRepository;
    private final TripRepository tripRepository;

    public MotorcycleService(
            MotorcycleRepository motorcycleRepository,
            TripRepository tripRepository
    ) {
        this.motorcycleRepository = motorcycleRepository;
        this.tripRepository = tripRepository;
    }

    public Motorcycle registerMotorcycle(
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity
    ) {
        Motorcycle motorcycle = Motorcycle.register(
                brand, model, color, year, engineCapacity
        );

        return motorcycleRepository.save(motorcycle);
    }

    public Motorcycle findMotorcycleById(Long motorcycleId) {
        return motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new MotorcycleNotFoundException(
                        "Motocicleta com id %d não encontrada".formatted(motorcycleId)
                ));
    }

    public List<Motorcycle> findAllMotorcycles() {
        return motorcycleRepository.findAll();
    }

    public void deleteMotorcycleById(Long motorcycleId) {
        findMotorcycleById(motorcycleId);

        if (tripRepository.existsByMotorcycleId(motorcycleId)) {
            throw new MotorcycleInUseException(
                    "A motocicleta não pode ser excluída enquanto possuir viagens associadas."
            );
        }

        motorcycleRepository.deleteById(motorcycleId);
    }

}
