package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MonthlyTripStatistics;
import br.dev.guisleri.mototrack.model.TerrainStatistics;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    void shouldCountCompletedTripsByOwnerAndMotorcycle() {
        Motorcycle savedHonda = save(completedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.minusDays(1),
                HONDA
        )).getMotorcycle();
        save(completedTrip(
                400,
                TerrainType.MIXED,
                TODAY.minusDays(2),
                savedHonda
        ));
        save(plannedTrip(
                900,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(1),
                savedHonda
        ));
        Motorcycle sameOwnerYamaha = Motorcycle.register(
                "Yamaha",
                "Tenere 700",
                "Blue",
                2024,
                689,
                savedHonda.getOwner()
        );
        save(completedTrip(
                1_000,
                TerrainType.OFF_ROAD,
                TODAY.minusDays(3),
                sameOwnerYamaha
        ));
        save(completedTrip(
                1_200,
                TerrainType.ASPHALT,
                TODAY.minusDays(4),
                YAMAHA
        ));

        Long result = tripStatisticsRepositoryAdapter
                .countByOwnerIdAndMotorcycleAndStatus(
                        savedHonda.getOwner().getId(),
                        savedHonda,
                        TripStatus.COMPLETED
                );

        assertEquals(2L, result);
    }

    @Test
    void shouldCalculateTerrainStatisticsForCompletedTripsAndOwner() {
        Motorcycle savedHonda = save(completedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.minusDays(1),
                HONDA
        )).getMotorcycle();
        save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.minusDays(2),
                savedHonda
        ));
        save(completedTrip(
                300,
                TerrainType.MIXED,
                TODAY.minusDays(3),
                savedHonda
        ));
        save(plannedTrip(
                900,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(1),
                savedHonda
        ));
        save(completedTrip(
                1_000,
                TerrainType.ASPHALT,
                TODAY.minusDays(4),
                YAMAHA
        ));
        Long ownerId = savedHonda.getOwner().getId();

        Map<TerrainType, TerrainStatistics> result =
                tripStatisticsRepositoryAdapter
                        .findTerrainStatisticsByOwnerId(ownerId);

        assertAll(
                () -> assertEquals(
                        2L,
                        result.get(TerrainType.ASPHALT).tripCount()
                ),
                () -> assertEquals(
                        300.0,
                        result.get(TerrainType.ASPHALT).totalDistanceKm(),
                        0.001
                ),
                () -> assertEquals(
                        1L,
                        result.get(TerrainType.MIXED).tripCount()
                ),
                () -> assertEquals(
                        300.0,
                        result.get(TerrainType.MIXED).totalDistanceKm(),
                        0.001
                )
        );
    }

    @Test
    void shouldCalculateTerrainStatisticsForCompletedTripsByMotorcycle() {
        Motorcycle savedHonda = save(completedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.minusDays(1),
                HONDA
        )).getMotorcycle();
        save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.minusDays(2),
                savedHonda
        ));
        save(completedTrip(
                300,
                TerrainType.MIXED,
                TODAY.minusDays(3),
                savedHonda
        ));
        save(plannedTrip(
                900,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(1),
                savedHonda
        ));
        Motorcycle sameOwnerYamaha = Motorcycle.register(
                "Yamaha",
                "Tenere 700",
                "Blue",
                2024,
                689,
                savedHonda.getOwner()
        );
        save(completedTrip(
                1_000,
                TerrainType.OFF_ROAD,
                TODAY.minusDays(4),
                sameOwnerYamaha
        ));

        Map<TerrainType, TerrainStatistics> result =
                tripStatisticsRepositoryAdapter
                        .findTerrainStatisticsByOwnerIdAndMotorcycle(
                                savedHonda.getOwner().getId(),
                                savedHonda
                        );

        assertAll(
                () -> assertEquals(
                        2L,
                        result.get(TerrainType.ASPHALT).tripCount()
                ),
                () -> assertEquals(
                        300.0,
                        result.get(TerrainType.ASPHALT).totalDistanceKm(),
                        0.001
                ),
                () -> assertEquals(
                        1L,
                        result.get(TerrainType.MIXED).tripCount()
                ),
                () -> assertEquals(
                        300.0,
                        result.get(TerrainType.MIXED).totalDistanceKm(),
                        0.001
                ),
                () -> assertEquals(
                        0L,
                        result.get(TerrainType.OFF_ROAD).tripCount()
                ),
                () -> assertEquals(
                        0.0,
                        result.get(TerrainType.OFF_ROAD).totalDistanceKm(),
                        0.001
                )
        );
    }

    @Test
    void shouldIncludeTerrainsWithZeroCompletedTrips() {
        Trip savedTrip = save(completedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY,
                HONDA
        ));
        Long ownerId = savedTrip.getMotorcycle().getOwner().getId();

        Map<TerrainType, TerrainStatistics> result =
                tripStatisticsRepositoryAdapter
                        .findTerrainStatisticsByOwnerId(ownerId);

        assertAll(
                () -> assertEquals(
                        0L,
                        result.get(TerrainType.MIXED).tripCount()
                ),
                () -> assertEquals(
                        0.0,
                        result.get(TerrainType.MIXED).totalDistanceKm(),
                        0.001
                ),
                () -> assertEquals(
                        0L,
                        result.get(TerrainType.OFF_ROAD).tripCount()
                ),
                () -> assertEquals(
                        0.0,
                        result.get(TerrainType.OFF_ROAD).totalDistanceKm(),
                        0.001
                )
        );
    }

    @Test
    void shouldCalculateMonthlyStatisticsForCompletedTripsSinceStartDateAndOwner() {
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        Motorcycle savedHonda = save(completedTrip(
                300,
                TerrainType.MIXED,
                LocalDate.of(2025, 12, 10),
                HONDA
        )).getMotorcycle();
        save(completedTrip(
                150,
                TerrainType.ASPHALT,
                LocalDate.of(2025, 10, 20),
                savedHonda
        ));
        save(completedTrip(
                100,
                TerrainType.OFF_ROAD,
                startDate,
                savedHonda
        ));
        save(completedTrip(
                1_000,
                TerrainType.ASPHALT,
                startDate.minusDays(1),
                savedHonda
        ));
        save(plannedTrip(
                2_000,
                TerrainType.MIXED,
                TODAY.plusDays(1),
                savedHonda
        ));
        save(completedTrip(
                3_000,
                TerrainType.OFF_ROAD,
                LocalDate.of(2025, 11, 10),
                YAMAHA
        ));

        List<MonthlyTripStatistics> result = tripStatisticsRepositoryAdapter
                .findMonthlyStatisticsByOwnerIdFromDate(
                        savedHonda.getOwner().getId(),
                        startDate
                );

        assertEquals(
                List.of(
                        new MonthlyTripStatistics(2025, 10, 2L, 250.0),
                        new MonthlyTripStatistics(2025, 12, 1L, 300.0)
                ),
                result
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

    @Test
    void shouldFindLongestCompletedTripForOwner() {
        Motorcycle savedHonda = save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.minusDays(3),
                HONDA
        )).getMotorcycle();
        Trip expected = save(completedTrip(
                500,
                TerrainType.MIXED,
                TODAY.minusDays(2),
                savedHonda
        ));
        save(plannedTrip(
                900,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(1),
                savedHonda
        ));
        save(completedTrip(
                1_200,
                TerrainType.ASPHALT,
                TODAY.minusDays(1),
                YAMAHA
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLongestCompletedTripByOwnerId(savedHonda.getOwner().getId())
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
    }

    @Test
    void shouldFindLongestCompletedTripForOwnerAndMotorcycle() {
        Motorcycle savedHonda = save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.minusDays(3),
                HONDA
        )).getMotorcycle();
        Trip expected = save(completedTrip(
                500,
                TerrainType.MIXED,
                TODAY.minusDays(2),
                savedHonda
        ));
        Motorcycle sameOwnerYamaha = Motorcycle.register(
                "Yamaha",
                "Tenere 700",
                "Blue",
                2024,
                689,
                savedHonda.getOwner()
        );
        save(completedTrip(
                900,
                TerrainType.OFF_ROAD,
                TODAY.minusDays(1),
                sameOwnerYamaha
        ));
        save(completedTrip(
                1_200,
                TerrainType.ASPHALT,
                TODAY.minusDays(1),
                YAMAHA
        ));
        save(plannedTrip(
                1_500,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(1),
                savedHonda
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLongestCompletedTripByOwnerIdAndMotorcycle(
                        savedHonda.getOwner().getId(),
                        savedHonda
                )
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
        assertEquals(500, result.getDistanceKm(), 0.001);
    }

    @Test
    void shouldReturnEmptyWhenMotorcycleHasNoCompletedTrips() {
        Motorcycle savedHonda = save(plannedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        )).getMotorcycle();

        assertTrue(tripStatisticsRepositoryAdapter
                .findLongestCompletedTripByOwnerIdAndMotorcycle(
                        savedHonda.getOwner().getId(),
                        savedHonda
                )
                .isEmpty());
    }

    @Test
    void shouldUseMostRecentTripThenHighestIdWhenLongestTripsTieByMotorcycle() {
        Motorcycle savedHonda = save(completedTrip(
                500,
                TerrainType.ASPHALT,
                LocalDate.of(2026, 9, 10),
                HONDA
        )).getMotorcycle();
        save(completedTrip(
                500,
                TerrainType.MIXED,
                LocalDate.of(2026, 9, 12),
                savedHonda
        ));
        Trip expected = save(completedTrip(
                500,
                TerrainType.OFF_ROAD,
                LocalDate.of(2026, 9, 12),
                savedHonda
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLongestCompletedTripByOwnerIdAndMotorcycle(
                        savedHonda.getOwner().getId(),
                        savedHonda
                )
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
    }

    @Test
    void shouldFindLastCompletedTripForOwner() {
        Motorcycle savedHonda = save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.minusDays(4),
                HONDA
        )).getMotorcycle();
        Trip expected = save(completedTrip(
                300,
                TerrainType.MIXED,
                TODAY,
                savedHonda
        ));
        save(plannedTrip(
                400,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(6),
                savedHonda
        ));
        save(completedTrip(
                500,
                TerrainType.ASPHALT,
                TODAY,
                YAMAHA
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLastCompletedTripByOwnerId(savedHonda.getOwner().getId())
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
    }

    @Test
    void shouldFindLastCompletedTripForOwnerAndMotorcycle() {
        Motorcycle savedHonda = save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.minusDays(5),
                HONDA
        )).getMotorcycle();
        Trip expected = save(completedTrip(
                300,
                TerrainType.MIXED,
                TODAY.minusDays(1),
                savedHonda
        ));
        Motorcycle sameOwnerYamaha = Motorcycle.register(
                "Yamaha",
                "Tenere 700",
                "Blue",
                2024,
                689,
                savedHonda.getOwner()
        );
        save(completedTrip(
                900,
                TerrainType.OFF_ROAD,
                TODAY,
                sameOwnerYamaha
        ));
        save(completedTrip(
                1_200,
                TerrainType.ASPHALT,
                TODAY,
                YAMAHA
        ));
        save(plannedTrip(
                1_500,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(1),
                savedHonda
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLastCompletedTripByOwnerIdAndMotorcycle(
                        savedHonda.getOwner().getId(),
                        savedHonda
                )
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
        assertEquals(TODAY.minusDays(1), result.getTripDate());
    }

    @Test
    void shouldUseHighestIdWhenLastCompletedTripsTieByMotorcycle() {
        LocalDate tripDate = LocalDate.of(2026, 9, 12);
        Motorcycle savedHonda = save(completedTrip(
                300,
                TerrainType.ASPHALT,
                tripDate,
                HONDA
        )).getMotorcycle();
        save(completedTrip(
                400,
                TerrainType.MIXED,
                tripDate,
                savedHonda
        ));
        Trip expected = save(completedTrip(
                500,
                TerrainType.OFF_ROAD,
                tripDate,
                savedHonda
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLastCompletedTripByOwnerIdAndMotorcycle(
                        savedHonda.getOwner().getId(),
                        savedHonda
                )
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
    }

    @Test
    void shouldReturnEmptyLastCompletedTripWhenMotorcycleHasNoCompletedTrips() {
        Motorcycle savedHonda = save(plannedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        )).getMotorcycle();

        Optional<Trip> result = tripStatisticsRepositoryAdapter
                .findLastCompletedTripByOwnerIdAndMotorcycle(
                        savedHonda.getOwner().getId(),
                        savedHonda
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindFirstCompletedTripForOwner() {
        Motorcycle savedHonda = save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY.minusDays(2),
                HONDA
        )).getMotorcycle();
        Trip expected = save(completedTrip(
                300,
                TerrainType.MIXED,
                TODAY.minusDays(4),
                savedHonda
        ));
        save(plannedTrip(
                400,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(6),
                savedHonda
        ));
        save(completedTrip(
                500,
                TerrainType.ASPHALT,
                TODAY.minusDays(10),
                YAMAHA
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findFirstCompletedTripByOwnerId(savedHonda.getOwner().getId())
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
    }

    @Test
    void shouldUseLowestIdWhenFirstCompletedTripsHaveSameDate() {
        Trip expected = save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY,
                HONDA
        ));
        save(completedTrip(
                300,
                TerrainType.MIXED,
                TODAY,
                expected.getMotorcycle()
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findFirstCompletedTripByOwnerId(
                        expected.getMotorcycle().getOwner().getId()
                )
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
    }

    @Test
    void shouldUseHighestIdWhenLastCompletedTripsHaveSameDate() {
        Motorcycle savedHonda = save(completedTrip(
                200,
                TerrainType.ASPHALT,
                TODAY,
                HONDA
        )).getMotorcycle();
        Trip expected = save(completedTrip(
                300,
                TerrainType.MIXED,
                TODAY,
                savedHonda
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLastCompletedTripByOwnerId(savedHonda.getOwner().getId())
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
    }

    @Test
    void shouldUseMostRecentTripWhenLongestTripsHaveSameDistance() {
        Motorcycle savedHonda = save(completedTrip(
                500,
                TerrainType.ASPHALT,
                LocalDate.of(2026, 9, 10),
                HONDA
        )).getMotorcycle();
        Trip expected = save(completedTrip(
                500,
                TerrainType.MIXED,
                LocalDate.of(2026, 9, 12),
                savedHonda
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLongestCompletedTripByOwnerId(savedHonda.getOwner().getId())
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
    }

    @Test
    void shouldUseHighestIdWhenLongestTripsHaveSameDistanceAndDate() {
        LocalDate tripDate = LocalDate.of(2026, 9, 12);
        Motorcycle savedHonda = save(completedTrip(
                500,
                TerrainType.ASPHALT,
                tripDate,
                HONDA
        )).getMotorcycle();
        Trip expected = save(completedTrip(
                500,
                TerrainType.MIXED,
                tripDate,
                savedHonda
        ));

        Trip result = tripStatisticsRepositoryAdapter
                .findLongestCompletedTripByOwnerId(savedHonda.getOwner().getId())
                .orElseThrow();

        assertEquals(expected.getId(), result.getId());
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
