package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.mapper.MotorcycleMapper;
import br.dev.guisleri.mototrack.persistence.mapper.TripMapper;
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
@Import({JpaTripRepositoryAdapter.class, TripMapper.class, MotorcycleMapper.class})
class JpaTripRepositoryAdapterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-14T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );
    private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);
    private static final Motorcycle HONDA = new Motorcycle(
            1,
            "Honda",
            "NX 500",
            2025,
            471
    );
    private static final Motorcycle YAMAHA = new Motorcycle(
            2,
            "Yamaha",
            "Tenere 700",
            2024,
            689
    );

    @Autowired
    private JpaTripRepositoryAdapter repository;

    @Autowired
    private SpringDataTripRepository tripRepository;

    @Autowired
    private SpringDataMotorcycleRepository motorcycleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistNewTripAndItsMotorcycleRelationship() {
        Trip newTrip = plannedTrip(175.5, TerrainType.MIXED, TODAY.plusDays(5), HONDA);

        assertNull(newTrip.getId());

        Trip savedTrip = repository.save(newTrip);
        tripRepository.flush();
        entityManager.clear();

        MotorcycleEntity savedMotorcycle = motorcycleRepository.findById(HONDA.getId())
                .orElseThrow();
        TripEntity savedEntity = tripRepository.findById(savedTrip.getId())
                .orElseThrow();

        assertAll(
                () -> assertNotNull(savedTrip.getId()),
                () -> assertEquals(HONDA.getId(), savedMotorcycle.getId()),
                () -> assertEquals("Honda", savedMotorcycle.getBrand()),
                () -> assertEquals(
                        savedMotorcycle.getId(),
                        savedEntity.getMotorcycle().getId()
                ),
                () -> assertEquals(savedTrip.getMotorcycle(), HONDA)
        );
    }

    @Test
    void shouldFindTripById() {
        Trip savedTrip = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                TODAY.plusDays(1),
                HONDA
        ));

        Trip result = repository.findById(savedTrip.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(savedTrip.getId(), result.getId()),
                () -> assertEquals(TripStatus.PLANNED, result.getStatus()),
                () -> assertEquals(HONDA, result.getMotorcycle())
        );
    }

    @Test
    void shouldReturnEmptyWhenTripDoesNotExist() {
        assertTrue(repository.findById(999).isEmpty());
    }

    @Test
    void shouldFindAllTrips() {
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

        List<Trip> result = repository.findAll();

        assertEquals(2, result.size());
        assertTrue(result.stream().map(Trip::getId).toList()
                .containsAll(List.of(firstTrip.getId(), secondTrip.getId())));
    }

    @Test
    void shouldFindTripsByStatus() {
        save(plannedTrip(100, TerrainType.ASPHALT, TODAY.plusDays(1), HONDA));
        Trip inProgressTrip = inProgressTrip(
                200,
                TerrainType.MIXED,
                TODAY.plusDays(2),
                YAMAHA
        );
        Trip savedInProgressTrip = save(inProgressTrip);

        List<Trip> result = repository.findByStatus(TripStatus.IN_PROGRESS);

        assertEquals(1, result.size());
        assertEquals(savedInProgressTrip.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindTripsByTerrain() {
        save(plannedTrip(100, TerrainType.ASPHALT, TODAY.plusDays(1), HONDA));
        Trip offRoadTrip = save(plannedTrip(
                200,
                TerrainType.OFF_ROAD,
                TODAY.plusDays(2),
                YAMAHA
        ));

        List<Trip> result = repository.findByTerrain(TerrainType.OFF_ROAD);

        assertEquals(1, result.size());
        assertEquals(offRoadTrip.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindTripsByDate() {
        LocalDate searchedDate = TODAY.plusDays(5);
        Trip tripOnDate = save(plannedTrip(
                100,
                TerrainType.ASPHALT,
                searchedDate,
                HONDA
        ));
        save(plannedTrip(200, TerrainType.MIXED, searchedDate.plusDays(1), YAMAHA));

        List<Trip> result = repository.findByTripDate(searchedDate);

        assertEquals(1, result.size());
        assertEquals(tripOnDate.getId(), result.getFirst().getId());
    }

    @Test
    void shouldFindOnlyPlannedUpcomingTripsInDateOrder() {
        LocalDate referenceDate = TODAY.plusDays(5);
        save(plannedTrip(100, TerrainType.ASPHALT, referenceDate.minusDays(1), HONDA));
        Trip laterTrip = save(plannedTrip(
                200,
                TerrainType.MIXED,
                referenceDate.plusDays(3),
                YAMAHA
        ));
        Trip closestTrip = save(plannedTrip(
                300,
                TerrainType.OFF_ROAD,
                referenceDate,
                HONDA
        ));
        save(inProgressTrip(
                400,
                TerrainType.ASPHALT,
                referenceDate.plusDays(1),
                YAMAHA
        ));

        List<Trip> result = repository.findUpcomingFrom(referenceDate);

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
    }

    @Test
    void shouldCalculateDistanceStatistics() {
        save(completedTrip(100.5, TerrainType.ASPHALT, TODAY.minusDays(1), HONDA));
        save(completedTrip(144.0, TerrainType.MIXED, TODAY.minusDays(2), HONDA));
        save(completedTrip(300, TerrainType.OFF_ROAD, TODAY.minusDays(3), YAMAHA));
        save(plannedTrip(500, TerrainType.ASPHALT, TODAY.plusDays(1), HONDA));

        assertAll(
                () -> assertEquals(
                        544.5,
                        repository.sumDistanceByStatus(TripStatus.COMPLETED),
                        0.001
                ),
                () -> assertEquals(
                        244.5,
                        repository.sumDistanceByMotorcycleAndStatus(
                                HONDA,
                                TripStatus.COMPLETED
                        ),
                        0.001
                )
        );
    }

    @Test
    void shouldCountTripsByStatusIncludingStatusesWithZeroTrips() {
        save(plannedTrip(100, TerrainType.ASPHALT, TODAY.plusDays(1), HONDA));

        Map<TripStatus, Long> result = repository.countByStatus();

        assertAll(
                () -> assertEquals(1L, result.get(TripStatus.PLANNED)),
                () -> assertEquals(0L, result.get(TripStatus.IN_PROGRESS)),
                () -> assertEquals(0L, result.get(TripStatus.COMPLETED))
        );
    }

    @Test
    void shouldCountTripsByMotorcycle() {
        save(plannedTrip(100, TerrainType.ASPHALT, TODAY.plusDays(1), HONDA));
        save(completedTrip(200, TerrainType.MIXED, TODAY.minusDays(1), HONDA));
        save(plannedTrip(300, TerrainType.OFF_ROAD, TODAY.plusDays(2), YAMAHA));

        Map<Motorcycle, Long> result = repository.countByMotorcycle();

        assertAll(
                () -> assertEquals(2L, result.get(HONDA)),
                () -> assertEquals(1L, result.get(YAMAHA))
        );
    }

    @Test
    void shouldFindMostUsedMotorcycleAmongCompletedTrips() {
        save(completedTrip(100, TerrainType.ASPHALT, TODAY.minusDays(1), HONDA));
        save(completedTrip(200, TerrainType.MIXED, TODAY.minusDays(2), HONDA));
        save(completedTrip(300, TerrainType.OFF_ROAD, TODAY.minusDays(3), YAMAHA));
        save(plannedTrip(400, TerrainType.ASPHALT, TODAY.plusDays(1), YAMAHA));
        save(plannedTrip(500, TerrainType.MIXED, TODAY.plusDays(2), YAMAHA));

        Motorcycle result = repository.findMostUsedMotorcycleInCompletedTrips()
                .orElseThrow();

        assertEquals(HONDA, result);
    }

    @Test
    void shouldReturnEmptyMostUsedMotorcycleWhenDatabaseIsEmpty() {
        assertTrue(repository.findMostUsedMotorcycleInCompletedTrips().isEmpty());
    }

    private Trip save(Trip trip) {
        Trip savedTrip = repository.save(trip);
        tripRepository.flush();
        return savedTrip;
    }

    private Trip plannedTrip(
            double distance,
            TerrainType terrain,
            LocalDate date,
            Motorcycle motorcycle
    ) {
        return Trip.schedule(
                "Origem",
                "Destino",
                distance,
                terrain,
                date,
                motorcycle,
                FIXED_CLOCK
        );
    }

    private Trip inProgressTrip(
            double distance,
            TerrainType terrain,
            LocalDate date,
            Motorcycle motorcycle
    ) {
        Trip trip = plannedTrip(distance, terrain, date, motorcycle);
        trip.changeStatus(TripStatus.IN_PROGRESS, TODAY);
        return trip;
    }

    private Trip completedTrip(
            double distance,
            TerrainType terrain,
            LocalDate date,
            Motorcycle motorcycle
    ) {
        return Trip.registerCompleted(
                "Origem",
                "Destino",
                distance,
                terrain,
                date,
                motorcycle,
                FIXED_CLOCK
        );
    }
}
