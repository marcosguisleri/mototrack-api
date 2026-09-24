package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.persistence.mapper.TripMapper;
import br.dev.guisleri.mototrack.persistence.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        JpaTripRepositoryAdapter.class,
        TripMapper.class,
        MotorcycleMapper.class,
        UserMapper.class
})
class JpaTripRepositoryAdapterTest {

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
    private SpringDataTripRepository springDataTripRepository;

    @Autowired
    private SpringDataMotorcycleRepository springDataMotorcycleRepository;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private MotorcycleMapper motorcycleMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistTripWithMotorcycleOwner() {
        Trip newTrip = plannedTrip(175.5, TerrainType.MIXED, TODAY.plusDays(5), HONDA);

        assertNull(newTrip.getId());
        assertNotNull(newTrip.getMotorcycle().getId());

        Trip savedTrip = tripRepositoryAdapter.save(newTrip);
        springDataTripRepository.flush();
        entityManager.clear();

        Long savedMotorcycleId = savedTrip.getMotorcycle().getId();
        Long savedOwnerId = savedTrip.getMotorcycle().getOwner().getId();
        MotorcycleEntity savedMotorcycle = springDataMotorcycleRepository.findById(savedMotorcycleId)
                .orElseThrow();
        UserEntity savedOwner = springDataUserRepository.findById(savedOwnerId)
                .orElseThrow();
        TripEntity savedTripEntity = springDataTripRepository.findById(savedTrip.getId())
                .orElseThrow();

        assertAll(
                () -> assertNotNull(savedTrip.getId()),
                () -> assertNotNull(savedMotorcycleId),
                () -> assertNotNull(savedOwnerId),
                () -> assertEquals(savedMotorcycleId, savedMotorcycle.getId()),
                () -> assertEquals("Honda", savedMotorcycle.getBrand()),
                () -> assertEquals("Black", savedMotorcycle.getColor()),
                () -> assertEquals(savedOwnerId, savedMotorcycle.getOwner().getId()),
                () -> assertEquals("Marcos", savedOwner.getName()),
                () -> assertEquals(
                        savedMotorcycle.getId(),
                        savedTripEntity.getMotorcycle().getId()
                ),
                () -> assertEquals(
                        savedOwnerId,
                        savedTripEntity.getMotorcycle().getOwner().getId()
                ),
                () -> assertEquals(savedMotorcycleId, savedTrip.getMotorcycle().getId()),
                () -> assertEquals(savedOwnerId, savedTrip.getMotorcycle().getOwner().getId())
        );
    }

    @Test
    void shouldFindTripByIdWithMotorcycleOwner() {
        Trip savedTrip = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        ));
        entityManager.clear();

        Trip result = tripRepositoryAdapter.findById(savedTrip.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(savedTrip.getId(), result.getId()),
                () -> assertEquals(TripStatus.PLANNED, result.getStatus()),
                () -> assertEquals(savedTrip.getMotorcycle(), result.getMotorcycle()),
                () -> assertNotNull(result.getMotorcycle()),
                () -> assertNotNull(result.getMotorcycle().getOwner()),
                () -> assertEquals(
                        savedTrip.getMotorcycle().getOwner().getId(),
                        result.getMotorcycle().getOwner().getId()
                ),
                () -> assertEquals("Marcos", result.getMotorcycle().getOwner().getName()),
                () -> assertEquals(
                        "marcos@example.com",
                        result.getMotorcycle().getOwner().getEmail()
                )
        );
    }

    @Test
    void shouldReturnEmptyWhenTripDoesNotExist() {
        assertTrue(tripRepositoryAdapter.findById(999).isEmpty());
    }

    @Test
    void shouldFindTripsByOwner() {
        Trip firstTrip = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        ));
        Trip secondTrip = save(plannedTrip(
                200,
                TerrainType.MIXED,
                TODAY.plusDays(2),
                YAMAHA
        ));
        entityManager.clear();

        Long ownerId = firstTrip.getMotorcycle().getOwner().getId();
        List<Trip> result = tripRepositoryAdapter.findByOwnerId(ownerId);

        assertEquals(1, result.size());
        assertEquals(firstTrip.getId(), result.getFirst().getId());
        assertTrue(result.stream()
                .map(Trip::getMotorcycle)
                .allMatch(motorcycle -> motorcycle.getOwner() != null));
        assertTrue(result.stream()
                .map(Trip::getMotorcycle)
                .map(Motorcycle::getOwner)
                .map(User::getId)
                .allMatch(ownerId::equals));
        assertTrue(result.stream().noneMatch(trip -> trip.getId().equals(secondTrip.getId())));
    }

    @Test
    void shouldFindTripsByOwnerAndStatus() {
        save(inProgressTrip(100, TerrainType.ASPHALT, TODAY.plusDays(1), HONDA));
        Trip inProgressTrip = inProgressTrip(
                200,
                TerrainType.MIXED,
                TODAY.plusDays(2),
                YAMAHA
        );
        Trip savedInProgressTrip = save(inProgressTrip);
        Long ownerId = savedInProgressTrip.getMotorcycle().getOwner().getId();

        List<Trip> result = tripRepositoryAdapter.findByOwnerIdAndStatus(
                ownerId,
                TripStatus.IN_PROGRESS
        );

        assertEquals(1, result.size());
        assertEquals(savedInProgressTrip.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindTripsByOwnerAndTerrain() {
        save(plannedTrip(100, TerrainType.OFF_ROAD, TODAY.plusDays(1), HONDA));
        Trip offRoadTrip = save(plannedTrip(
                200,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(2),
                YAMAHA
        ));
        Long ownerId = offRoadTrip.getMotorcycle().getOwner().getId();

        List<Trip> result = tripRepositoryAdapter.findByOwnerIdAndTerrain(
                ownerId,
                TerrainType.OFF_ROAD
        );

        assertEquals(1, result.size());
        assertEquals(offRoadTrip.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindTripsByOwnerAndDate() {
        LocalDate searchedDate = TODAY.plusDays(5);
        Trip tripOnDate = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                searchedDate,
                HONDA
        ));
        save(plannedTrip(200, TerrainType.MIXED, searchedDate, YAMAHA));
        Long ownerId = tripOnDate.getMotorcycle().getOwner().getId();

        List<Trip> result = tripRepositoryAdapter.findByOwnerIdAndTripDate(
                ownerId,
                searchedDate
        );

        assertEquals(1, result.size());
        assertEquals(tripOnDate.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindOnlyOwnersPlannedUpcomingTripsInDateOrder() {
        LocalDate referenceDate = TODAY.plusDays(5);
        save(plannedTrip(100, TerrainType.ASPHALT, referenceDate.minusDays(1), HONDA));
        Trip laterTrip = save(plannedTrip(
                200,
                TerrainType.MIXED,
                referenceDate.plusDays(3),
                HONDA
        ));
        Trip closestTrip = save(plannedTrip(
                300,
                TerrainType.OFF_ROAD,
                referenceDate,
                HONDA
        ));
        save(plannedTrip(
                400,
                TerrainType.ASPHALT,
                referenceDate.plusDays(1),
                YAMAHA
        ));
        save(inProgressTrip(
                500,
                TerrainType.MIXED,
                referenceDate.plusDays(2),
                HONDA
        ));
        Long ownerId = closestTrip.getMotorcycle().getOwner().getId();

        List<Trip> result = tripRepositoryAdapter.findUpcomingFromByOwnerId(
                ownerId,
                referenceDate
        );

        assertEquals(
                List.of(closestTrip.getId(), laterTrip.getId()),
                result.stream().map(Trip::getId).toList()
        );
        assertTrue(result.stream().allMatch(
                trip -> trip.getStatus() == TripStatus.PLANNED
        ));
        assertTrue(result.stream().noneMatch(
                trip -> trip.getTripDate().isBefore(referenceDate)
        ));
        assertTrue(result.stream().allMatch(
                trip -> trip.getMotorcycle().getOwner().getId().equals(ownerId)
        ));
    }

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

        assertAll(
                () -> assertEquals(
                        544.5,
                        tripRepositoryAdapter.sumDistanceKmByStatus(TripStatus.COMPLETED),
                        0.001
                ),
                () -> assertEquals(
                        244.5,
                        tripRepositoryAdapter.sumDistanceKmByMotorcycleAndStatus(
                                savedHonda,
                                TripStatus.COMPLETED
                        ),
                        0.001
                )
        );
    }

    @Test
    void shouldCountTripsByStatusIncludingStatusesWithZeroTrips() {
        save(plannedTrip(100, TerrainType.ASPHALT, TODAY.plusDays(1), HONDA));

        Map<TripStatus, Long> result = tripRepositoryAdapter.countByStatus();

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

        Map<Motorcycle, Long> result = tripRepositoryAdapter.countByMotorcycle();

        assertAll(
                () -> assertEquals(2L, result.get(savedHonda)),
                () -> assertEquals(1L, result.get(savedYamaha))
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
        Motorcycle savedYamaha = save(completedTrip(
                300,
                TerrainType.OFF_ROAD,
                TODAY.minusDays(3),
                YAMAHA
        )).getMotorcycle();
        save(plannedTrip(400, TerrainType.ASPHALT, TODAY.plusDays(1), savedYamaha));
        save(plannedTrip(500, TerrainType.MIXED, TODAY.plusDays(2), savedYamaha));

        Motorcycle result = tripRepositoryAdapter.findMostUsedMotorcycleInCompletedTrips()
                .orElseThrow();

        assertEquals(savedHonda, result);
    }

    @Test
    void shouldReturnEmptyMostUsedMotorcycleWhenDatabaseIsEmpty() {
        assertTrue(tripRepositoryAdapter.findMostUsedMotorcycleInCompletedTrips().isEmpty());
    }

    @Test
    void shouldCheckWhetherMotorcycleHasTrips() {
        Trip savedTrip = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        ));

        assertTrue(tripRepositoryAdapter.existsByMotorcycleId(
                savedTrip.getMotorcycle().getId()
        ));
    }

    @Test
    void shouldDeleteTripById() {
        Trip savedTrip = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        ));

        tripRepositoryAdapter.deleteById(savedTrip.getId());
        springDataTripRepository.flush();

        assertTrue(tripRepositoryAdapter.findById(savedTrip.getId()).isEmpty());
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

    private Trip inProgressTrip(
            double distanceKm,
            TerrainType terrainType,
            LocalDate tripDate,
            Motorcycle motorcycle
    ) {
        Trip trip = plannedTrip(distanceKm, terrainType, tripDate, motorcycle);
        trip.changeStatus(TripStatus.IN_PROGRESS, TODAY);
        return trip;
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
