package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTripRepositoryTest {

    private InMemoryTripRepository repository;
    private Motorcycle honda;
    private Motorcycle yamaha;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTripRepository();
        honda = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
        yamaha = new Motorcycle(2, "Yamaha", "Tenere 700", 2024, 689);
    }

    @Test
    void shouldSaveTrip() {
        Trip trip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);

        repository.save(trip);

        assertSame(trip, repository.findById(1).orElseThrow());
    }

    @Test
    void shouldFindTripById() {
        Trip trip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        repository.save(trip);

        Trip result = repository.findById(1).orElseThrow();

        assertSame(trip, result);
    }

    @Test
    void shouldReturnEmptyWhenTripDoesNotExist() {
        assertTrue(repository.findById(99).isEmpty());
    }

    @Test
    void shouldUpdateTripWhenSavingExistingId() {
        Trip original = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip updated = createTrip(1, 250, TerrainType.MIXED, 2, yamaha);
        repository.save(original);

        repository.save(updated);

        assertEquals(1, repository.findAll().size());
        assertSame(updated, repository.findById(1).orElseThrow());
    }

    @Test
    void shouldFindAllTrips() {
        Trip firstTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip secondTrip = createTrip(2, 200, TerrainType.MIXED, 2, yamaha);
        repository.save(firstTrip);
        repository.save(secondTrip);

        List<Trip> result = repository.findAll();

        assertEquals(List.of(firstTrip, secondTrip), result);
    }

    @Test
    void shouldFindTripsByTerrain() {
        Trip asphaltTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip offRoadTrip = createTrip(2, 200, TerrainType.OFF_ROAD, 2, yamaha);
        repository.save(asphaltTrip);
        repository.save(offRoadTrip);

        List<Trip> result = repository.findByTerrain(TerrainType.OFF_ROAD);

        assertEquals(1, result.size());
        assertSame(offRoadTrip, result.getFirst());
    }

    @Test
    void shouldFindTripsByStatus() {
        Trip plannedTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip inProgressTrip = createTrip(2, 200, TerrainType.MIXED, 2, yamaha);
        inProgressTrip.changeStatus(TripStatus.IN_PROGRESS);
        repository.save(plannedTrip);
        repository.save(inProgressTrip);

        List<Trip> result = repository.findByStatus(TripStatus.IN_PROGRESS);

        assertEquals(1, result.size());
        assertSame(inProgressTrip, result.getFirst());
    }

    @Test
    void shouldCountTripsByStatus() {
        Trip plannedTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip inProgressTrip = createTrip(2, 200, TerrainType.MIXED, 2, yamaha);
        Trip completedTrip = createTrip(3, 300, TerrainType.OFF_ROAD, 3, honda);
        inProgressTrip.changeStatus(TripStatus.IN_PROGRESS);
        complete(completedTrip);
        repository.save(plannedTrip);
        repository.save(inProgressTrip);
        repository.save(completedTrip);

        Map<TripStatus, Long> result = repository.countByStatus();

        assertEquals(1L, result.get(TripStatus.PLANNED));
        assertEquals(1L, result.get(TripStatus.IN_PROGRESS));
        assertEquals(1L, result.get(TripStatus.COMPLETED));
    }

    @Test
    void shouldFindTripsByPlannedDate() {
        LocalDate searchedDate = LocalDate.now().plusDays(5);
        Trip tripOnDate = createTrip(1, 100, TerrainType.ASPHALT, searchedDate, honda);
        Trip tripOnOtherDate = createTrip(2, 200, TerrainType.MIXED, searchedDate.plusDays(1), yamaha);
        repository.save(tripOnDate);
        repository.save(tripOnOtherDate);

        List<Trip> result = repository.findByPlannedDate(searchedDate);

        assertEquals(1, result.size());
        assertSame(tripOnDate, result.getFirst());
    }

    @Test
    void shouldFindUpcomingTrips() {
        LocalDate referenceDate = LocalDate.now().plusDays(5);
        Trip earlierTrip = createTrip(1, 100, TerrainType.ASPHALT, referenceDate.minusDays(1), honda);
        Trip tripOnReferenceDate = createTrip(2, 200, TerrainType.MIXED, referenceDate, yamaha);
        Trip futureTrip = createTrip(3, 300, TerrainType.OFF_ROAD, referenceDate.plusDays(1), honda);
        repository.save(earlierTrip);
        repository.save(tripOnReferenceDate);
        repository.save(futureTrip);

        List<Trip> result = repository.findUpcomingFrom(referenceDate);

        assertEquals(List.of(tripOnReferenceDate, futureTrip), result);
    }

    @Test
    void shouldIgnoreCompletedTripsWhenFindingUpcomingTrips() {
        LocalDate today = LocalDate.now();
        Trip completedTrip = createTrip(1, 100, TerrainType.ASPHALT, today, honda);
        Trip plannedTrip = createTrip(2, 200, TerrainType.MIXED, today.plusDays(1), yamaha);
        complete(completedTrip);
        repository.save(completedTrip);
        repository.save(plannedTrip);

        List<Trip> result = repository.findUpcomingFrom(today);

        assertEquals(1, result.size());
        assertSame(plannedTrip, result.getFirst());
        assertFalse(result.contains(completedTrip));
    }

    @Test
    void shouldSortUpcomingTripsByDate() {
        LocalDate today = LocalDate.now();
        Trip laterTrip = createTrip(1, 100, TerrainType.ASPHALT, today.plusDays(10), honda);
        Trip closestTrip = createTrip(2, 200, TerrainType.MIXED, today.plusDays(2), yamaha);
        Trip middleTrip = createTrip(3, 300, TerrainType.OFF_ROAD, today.plusDays(5), honda);
        repository.save(laterTrip);
        repository.save(closestTrip);
        repository.save(middleTrip);

        List<Trip> result = repository.findUpcomingFrom(today);

        assertEquals(List.of(closestTrip, middleTrip, laterTrip), result);
        assertSame(closestTrip, result.getFirst());
    }

    @Test
    void shouldSumDistanceByStatus() {
        Trip firstCompletedTrip = createTrip(1, 100.5, TerrainType.ASPHALT, 1, honda);
        Trip secondCompletedTrip = createTrip(2, 149.5, TerrainType.MIXED, 2, yamaha);
        Trip plannedTrip = createTrip(3, 500, TerrainType.OFF_ROAD, 3, honda);
        complete(firstCompletedTrip);
        complete(secondCompletedTrip);
        repository.save(firstCompletedTrip);
        repository.save(secondCompletedTrip);
        repository.save(plannedTrip);

        double result = repository.sumDistanceByStatus(TripStatus.COMPLETED);

        assertEquals(250, result, 0.001);
    }

    @Test
    void shouldCountTripsByMotorcycle() {
        repository.save(createTrip(1, 100, TerrainType.ASPHALT, 1, honda));
        repository.save(createTrip(2, 200, TerrainType.MIXED, 2, honda));
        repository.save(createTrip(3, 300, TerrainType.OFF_ROAD, 3, yamaha));

        Map<Motorcycle, Long> result = repository.countByMotorcycle();

        assertEquals(2L, result.get(honda));
        assertEquals(1L, result.get(yamaha));
    }

    @Test
    void shouldSumDistanceByMotorcycleAndStatus() {
        Trip completedHondaTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip plannedHondaTrip = createTrip(2, 200, TerrainType.MIXED, 2, honda);
        Trip completedYamahaTrip = createTrip(3, 300, TerrainType.OFF_ROAD, 3, yamaha);
        complete(completedHondaTrip);
        complete(completedYamahaTrip);
        repository.save(completedHondaTrip);
        repository.save(plannedHondaTrip);
        repository.save(completedYamahaTrip);

        double result = repository.sumDistanceByMotorcycleAndStatus(
                honda,
                TripStatus.COMPLETED
        );

        assertEquals(100, result, 0.001);
    }

    private Trip createTrip(
            long id,
            double distance,
            TerrainType terrain,
            int daysFromToday,
            Motorcycle motorcycle
    ) {
        return createTrip(
                id,
                distance,
                terrain,
                LocalDate.now().plusDays(daysFromToday),
                motorcycle
        );
    }

    private Trip createTrip(
            long id,
            double distance,
            TerrainType terrain,
            LocalDate plannedDate,
            Motorcycle motorcycle
    ) {
        return new Trip(
                id,
                "Origem " + id,
                "Destino " + id,
                distance,
                terrain,
                plannedDate,
                motorcycle
        );
    }

    private void complete(Trip trip) {
        trip.changeStatus(TripStatus.IN_PROGRESS);
        trip.changeStatus(TripStatus.COMPLETED);
    }
}
