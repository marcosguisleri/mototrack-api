package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
@Import({JpaMotorcycleRepositoryAdapter.class, MotorcycleMapper.class})
class JpaMotorcycleRepositoryAdapterTest {

    @Autowired
    private JpaMotorcycleRepositoryAdapter motorcycleRepositoryAdapter;

    @Autowired
    private SpringDataMotorcycleRepository springDataMotorcycleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistNewMotorcycle() {
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
                () -> assertEquals("Honda", savedMotorcycle.getBrand()),
                () -> assertEquals("NX 500", savedMotorcycle.getModel()),
                () -> assertEquals("Black", savedMotorcycle.getColor()),
                () -> assertEquals(2025, savedMotorcycle.getYear()),
                () -> assertEquals(471, savedMotorcycle.getEngineCapacity())
        );
    }

    @Test
    void shouldFindMotorcycleById() {
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
                () -> assertEquals(471, result.getEngineCapacity())
        );
    }

    @Test
    void shouldReturnEmptyWhenMotorcycleDoesNotExist() {
        assertTrue(motorcycleRepositoryAdapter.findById(999L).isEmpty());
    }

    @Test
    void shouldFindAllMotorcycles() {
        Motorcycle honda = save(motorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        ));
        Motorcycle yamaha = save(motorcycle(
                "Yamaha",
                "Tenere 700",
                "Blue",
                2024,
                689
        ));
        entityManager.clear();

        List<Motorcycle> result = motorcycleRepositoryAdapter.findAll();

        assertEquals(2, result.size());
        assertTrue(result.stream().map(Motorcycle::getId).toList()
                .containsAll(List.of(honda.getId(), yamaha.getId())));
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
        return Motorcycle.register(brand, model, color, year, engineCapacity);
    }
}
