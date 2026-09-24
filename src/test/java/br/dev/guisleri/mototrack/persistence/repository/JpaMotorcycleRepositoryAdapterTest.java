package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.persistence.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        JpaMotorcycleRepositoryAdapter.class,
        MotorcycleMapper.class,
        UserMapper.class
})
class JpaMotorcycleRepositoryAdapterTest {

    @Autowired
    private JpaMotorcycleRepositoryAdapter motorcycleRepositoryAdapter;

    @Autowired
    private SpringDataMotorcycleRepository springDataMotorcycleRepository;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = saveUser("Marcos", "marcos@example.com");
    }

    @Test
    void shouldPersistMotorcycleWithOwner() {
        Motorcycle newMotorcycle = motorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        );

        assertNull(newMotorcycle.getId());

        Motorcycle savedMotorcycle = motorcycleRepositoryAdapter.save(newMotorcycle);
        springDataMotorcycleRepository.flush();
        entityManager.clear();

        MotorcycleEntity savedEntity = springDataMotorcycleRepository
                .findById(savedMotorcycle.getId())
                .orElseThrow();

        assertAll(
                () -> assertNotNull(savedMotorcycle.getId()),
                () -> assertEquals(savedMotorcycle.getId(), savedEntity.getId()),
                () -> assertEquals("Honda", savedEntity.getBrand()),
                () -> assertEquals("NX 500", savedEntity.getModel()),
                () -> assertEquals("Black", savedEntity.getColor()),
                () -> assertEquals(2025, savedEntity.getYear()),
                () -> assertEquals(471, savedEntity.getEngineCapacity()),
                () -> assertEquals(owner.getId(), savedEntity.getOwner().getId()),
                () -> assertEquals("Honda", savedMotorcycle.getBrand()),
                () -> assertEquals("NX 500", savedMotorcycle.getModel()),
                () -> assertEquals("Black", savedMotorcycle.getColor()),
                () -> assertEquals(2025, savedMotorcycle.getYear()),
                () -> assertEquals(471, savedMotorcycle.getEngineCapacity()),
                () -> assertEquals(owner.getId(), savedMotorcycle.getOwner().getId()),
                () -> assertEquals(
                        "marcos@example.com",
                        savedMotorcycle.getOwner().getEmail()
                )
        );
    }

    @Test
    void shouldRestoreMotorcycleWithOwner() {
        Motorcycle savedMotorcycle = save(motorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        ));
        entityManager.clear();

        Motorcycle result = motorcycleRepositoryAdapter
                .findById(savedMotorcycle.getId())
                .orElseThrow();

        assertAll(
                () -> assertEquals(savedMotorcycle.getId(), result.getId()),
                () -> assertEquals("Honda", result.getBrand()),
                () -> assertEquals("NX 500", result.getModel()),
                () -> assertEquals("Black", result.getColor()),
                () -> assertEquals(2025, result.getYear()),
                () -> assertEquals(471, result.getEngineCapacity()),
                () -> assertEquals(owner.getId(), result.getOwner().getId()),
                () -> assertEquals("Marcos", result.getOwner().getName()),
                () -> assertEquals("marcos@example.com", result.getOwner().getEmail())
        );
    }

    @Test
    void shouldReturnEmptyWhenMotorcycleDoesNotExist() {
        assertTrue(motorcycleRepositoryAdapter.findById(999L).isEmpty());
    }

    @Test
    void shouldFindMotorcyclesByOwnerId() {
        Motorcycle firstOwnerMotorcycle = save(motorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        ));
        Motorcycle secondOwnerMotorcycle = save(motorcycle(
                "Honda",
                "CRF 1100L",
                "Red",
                2024,
                1084
        ));
        User anotherOwner = saveUser("Ana", "ana@example.com");
        Motorcycle anotherOwnerMotorcycle = save(motorcycle(
                "Dafra",
                "NH 300",
                "Red",
                2025,
                291,
                anotherOwner
        ));
        entityManager.clear();

        List<Motorcycle> firstOwnerGarage = motorcycleRepositoryAdapter
                .findByOwnerId(owner.getId());
        List<Motorcycle> secondOwnerGarage = motorcycleRepositoryAdapter
                .findByOwnerId(anotherOwner.getId());

        assertEquals(2, firstOwnerGarage.size());
        assertTrue(firstOwnerGarage.stream().map(Motorcycle::getId).toList()
                .containsAll(List.of(
                        firstOwnerMotorcycle.getId(),
                        secondOwnerMotorcycle.getId()
                )));
        assertTrue(firstOwnerGarage.stream().allMatch(
                motorcycle -> motorcycle.getOwner().getId().equals(owner.getId())
        ));
        assertEquals(1, secondOwnerGarage.size());
        assertEquals(anotherOwnerMotorcycle.getId(), secondOwnerGarage.getFirst().getId());
        assertEquals("Dafra", secondOwnerGarage.getFirst().getBrand());
        assertEquals(anotherOwner.getId(), secondOwnerGarage.getFirst().getOwner().getId());
    }

    @Test
    void shouldReturnEmptyWhenOwnerHasNoMotorcycles() {
        assertTrue(motorcycleRepositoryAdapter
                .findByOwnerId(owner.getId())
                .isEmpty());
    }

    @Test
    void shouldDeleteMotorcycleById() {
        Motorcycle savedMotorcycle = save(motorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        ));

        motorcycleRepositoryAdapter.deleteById(savedMotorcycle.getId());
        springDataMotorcycleRepository.flush();

        assertTrue(motorcycleRepositoryAdapter
                .findById(savedMotorcycle.getId())
                .isEmpty());
    }

    private Motorcycle save(Motorcycle motorcycle) {
        Motorcycle savedMotorcycle = motorcycleRepositoryAdapter.save(motorcycle);
        springDataMotorcycleRepository.flush();
        return savedMotorcycle;
    }

    private Motorcycle motorcycle(
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity
    ) {
        return motorcycle(brand, model, color, year, engineCapacity, owner);
    }

    private Motorcycle motorcycle(
            String brand,
            String model,
            String color,
            int year,
            int engineCapacity,
            User motorcycleOwner
    ) {
        return Motorcycle.register(
                brand,
                model,
                color,
                year,
                engineCapacity,
                motorcycleOwner
        );
    }

    private User saveUser(String name, String email) {
        UserEntity userEntity = springDataUserRepository.saveAndFlush(
                new UserEntity(null, name, email, "password-hash")
        );
        return User.restore(
                userEntity.getId(),
                userEntity.getName(),
                userEntity.getEmail(),
                userEntity.getPasswordHash()
        );
    }
}
