package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.MotorcycleInUseException;
import br.dev.guisleri.mototrack.exception.MotorcycleNotFoundException;
import br.dev.guisleri.mototrack.exception.UserNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.MotorcycleRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MotorcycleService {

    private final MotorcycleRepository motorcycleRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public MotorcycleService(
            MotorcycleRepository motorcycleRepository,
            TripRepository tripRepository,
            UserRepository userRepository
    ) {
        this.motorcycleRepository = motorcycleRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    public Motorcycle registerMotorcycle(
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity,
            Long ownerId
    ) {

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com id %d não encontrado"
                                .formatted(ownerId)
                ));

        Motorcycle motorcycle = Motorcycle.register(
                brand, model, color, year, engineCapacity, user
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

    public List<Motorcycle> findMotorcyclesByOwnerId(Long ownerId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário com id %d não encontrado"
                                .formatted(ownerId)
                ));

        return motorcycleRepository.findByOwnerId(ownerId);
    }

}
