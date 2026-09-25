package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.persistence.mapper.TripMapper;
import br.dev.guisleri.mototrack.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        JpaTripRepositoryAdapter.class,
        JpaTripStatisticsRepositoryAdapter.class,
        TripMapper.class,
        MotorcycleMapper.class,
        UserMapper.class
})
class JpaTripStatisticsRepositoryAdapterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-14T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );
    private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);
    private static final Motorcycle HONDA = Motorcycle.register(
            "Honda",
            "NX 500",
            "Black",
            2025,
            471,
            User.register("Marcos", "marcos@example.com", "password-hash")
    );
    private static final Motorcycle YAMAHA = Motorcycle.register(
            "Yamaha",
            "Tenere 700",
            "Blue",
            2024,
            689,
            User.register("Ana", "ana@example.com", "password-hash")
    );

    @Autowired
    private JpaTripRepositoryAdapter tripRepositoryAdapter;

    @Autowired
    private JpaTripStatisticsRepositoryAdapter tripStatisticsRepositoryAdapter;

    @Autowired
    private SpringDataTripRepository springDataTripRepository;

    @Autowired
    private SpringDataMotorcycleRepository springDataMotorcycleRepository;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private MotorcycleMapper motorcycleMapper;

    @Test
    void shouldCalculateDistanceStatistics() {
        Motorcycle savedHonda = save(completedTrip(
                100.5,
                TerrainType.ASPHALT,
                TODAY.minusDays(1),
                HONDA
        )).getMotorcycle();
        save(completedTrip(144.0, TerrainType.MIXED, TODAY.minusDays(2), savedHonda));
        save(completedTrip(300, TerrainType.OFF_ROAD, TODAY.minusDays(3), YAMAHA));
        save(plannedTrip(500, TerrainType.ASPHALT, TODAY.plusDays(1), savedHonda));
        Long ownerId = savedHonda.getOwner().getId();

        assertAll(
                () -> assertEquals(
                        244.5,
                        tripStatisticsRepositoryAdapter.sumDistanceKmByOwnerIdAndStatus(
                                ownerId,
                                TripStatus.COMPLETED
                        ),
                        0.001
                ),
                () -> assertEquals(
                        244.5,
                        tripStatisticsRepositoryAdapter
                                .sumDistanceKmByOwnerIdAndMotorcycleAndStatus(
                                        ownerId,
                                        savedHonda,
                                        TripStatus.COMPLETED
                                ),
                        0.001
                )
        );
    }

    @Test
    void shouldCountTripsByStatusIncludingStatusesWithZeroTrips() {
        Trip savedTrip = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        ));
        save(completedTrip(200, TerrainType.MIXED, TODAY.minusDays(1), YAMAHA));
        Long ownerId = savedTrip.getMotorcycle().getOwner().getId();

        Map<TripStatus, Long> result =
                tripStatisticsRepositoryAdapter.countByOwnerIdAndStatus(ownerId);

        assertAll(
                () -> assertEquals(1L, result.get(TripStatus.PLANNED)),
                () -> assertEquals(0L, result.get(TripStatus.IN_PROGRESS)),
                () -> assertEquals(0L, result.get(TripStatus.COMPLETED))
        );
    }

    @Test
    void shouldCountTripsByMotorcycle() {
        Motorcycle savedHonda = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        )).getMotorcycle();
        save(completedTrip(200, TerrainType.MIXED, TODAY.minusDays(1), savedHonda));
        Motorcycle savedYamaha = save(plannedTrip(
                300,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(2),
                YAMAHA
        )).getMotorcycle();

        Long ownerId = savedHonda.getOwner().getId();
        Map<Motorcycle, Long> result =
                tripStatisticsRepositoryAdapter.countByOwnerIdAndMotorcycle(ownerId);

        assertAll(
                () -> assertEquals(2L, result.get(savedHonda)),
                () -> assertEquals(1, result.size()),
                () -> assertFalse(result.containsKey(savedYamaha))
        );
    }

    @Test
    void shouldFindMostUsedMotorcycleAmongCompletedTrips() {
        Motorcycle savedHonda = save(completedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.minusDays(1),
                HONDA
        )).getMotorcycle();
        save(completedTrip(200, TerrainType.MIXED, TODAY.minusDays(2), savedHonda));
        Motorcycle sameOwnerYamaha = Motorcycle.register(
                "Yamaha",
                "Tenere 700",
                "Blue",
                2024,
                689,
                savedHonda.getOwner()
        );
        Motorcycle savedYamaha = save(completedTrip(
                300,
                TerrainType.OFF_ROAD,
                TODAY.minusDays(3),
                sameOwnerYamaha
        )).getMotorcycle();
        save(plannedTrip(400, TerrainType.ASPHALT, TODAY.plusDays(1), savedYamaha));
        save(plannedTrip(500, TerrainType.MIXED, TODAY.plusDays(2), savedYamaha));

        Motorcycle result = tripStatisticsRepositoryAdapter
                .findMostUsedMotorcycleInCompletedTripsByOwnerId(
                        savedHonda.getOwner().getId()
                )
                .orElseThrow();

        assertEquals(savedHonda, result);
    }

    @Test
    void shouldReturnEmptyMostUsedMotorcycleWhenDatabaseIsEmpty() {
        assertTrue(tripStatisticsRepositoryAdapter
                .findMostUsedMotorcycleInCompletedTripsByOwnerId(999L)
                .isEmpty());
    }

    private Trip save(Trip trip) {
        Trip savedTrip = tripRepositoryAdapter.save(trip);
        springDataTripRepository.flush();
        return savedTrip;
    }

    private Trip plannedTrip(
            double distanceKm,
            TerrainType terrainType,
            LocalDate tripDate,
            Motorcycle motorcycle
    ) {
        Motorcycle persistedMotorcycle = persistMotorcycleIfNecessary(motorcycle);

        return Trip.schedule(
                "Origem",
                "Destino",
                distanceKm,
                terrainType,
                tripDate,
                persistedMotorcycle,
                FIXED_CLOCK
        );
    }

    private Trip completedTrip(
            double distanceKm,
            TerrainType terrainType,
            LocalDate tripDate,
            Motorcycle motorcycle
    ) {
        Motorcycle persistedMotorcycle = persistMotorcycleIfNecessary(motorcycle);

        return Trip.registerCompleted(
                "Origem",
                "Destino",
                distanceKm,
                terrainType,
                tripDate,
                persistedMotorcycle,
                FIXED_CLOCK
        );
    }

    private Motorcycle persistMotorcycleIfNecessary(Motorcycle motorcycle) {
        if (motorcycle.getId() != null) {
            return motorcycle;
        }

        User owner = motorcycle.getOwner();
        UserEntity ownerEntity = springDataUserRepository
                .findByEmail(owner.getEmail())
                .orElseGet(() -> springDataUserRepository.saveAndFlush(
                        new UserEntity(
                                null,
                                owner.getName(),
                                owner.getEmail(),
                                owner.getPasswordHash()
                        )
                ));
        MotorcycleEntity savedMotorcycleEntity = springDataMotorcycleRepository.saveAndFlush(
                motorcycleMapper.toEntity(motorcycle, ownerEntity)
        );
        return motorcycleMapper.toDomain(savedMotorcycleEntity);
    }
}
