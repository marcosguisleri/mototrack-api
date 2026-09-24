package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.MotorcycleAccessDeniedException;
import br.dev.guisleri.mototrack.exception.MotorcycleInUseException;
import br.dev.guisleri.mototrack.exception.MotorcycleNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.User;
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
            int engineCapacity,
            User owner
    ) {

        Motorcycle motorcycle = Motorcycle.register(
                brand, model, color, year, engineCapacity, owner
        );

        return motorcycleRepository.save(motorcycle);
    }

    public void deleteMotorcycleByIdForOwner(Long motorcycleId, Long ownerId) {
        findMotorcycleByIdForOwner(motorcycleId, ownerId);

        if (tripRepository.existsByMotorcycleId(motorcycleId)) {
            throw new MotorcycleInUseException(
                    "A motocicleta não pode ser excluída enquanto possuir viagens associadas."
            );
        }

        motorcycleRepository.deleteById(motorcycleId);
    }

    public List<Motorcycle> findMotorcyclesByOwnerId(Long ownerId) {
        return motorcycleRepository.findByOwnerId(ownerId);
    }

    public Motorcycle findMotorcycleByIdForOwner(
            Long motorcycleId,
            Long ownerId
    ) {
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new MotorcycleNotFoundException(
                        "Motocicleta com id %d não encontrada".formatted(motorcycleId)
                ));

        if (!motorcycle.getOwner().getId().equals(ownerId)) {
            throw new MotorcycleAccessDeniedException(
                    "Você não possui permissão para acessar esta motocicleta."
            );
        }

        return motorcycle;
    }

}
