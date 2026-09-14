package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.InMemoryTripRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripServiceTest {

    private TripService service;
    private Motorcycle honda;
    private Motorcycle yamaha;

    @BeforeEach
    void setUp() {
        TripRepository repository = new InMemoryTripRepository();
        service = new TripService(repository);
        honda = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
        yamaha = new Motorcycle(2, "Yamaha", "Tenere 700", 2024, 689);
    }

    @Test
    void shouldRegisterTrip() {
        Trip trip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);

        service.registerTrip(trip);

        assertSame(trip, service.findTripById(1).orElseThrow());
    }

    @Test
    void shouldFindTripById() {
        Trip trip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        service.registerTrip(trip);

        Trip result = service.findTripById(1).orElseThrow();

        assertSame(trip, result);
    }

    @Test
    void shouldChangeTripStatus() {
        Trip trip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        service.registerTrip(trip);

        service.changeTripStatus(1, TripStatus.IN_PROGRESS);

        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
        assertSame(trip, service.findTripById(1).orElseThrow());
    }

    @Test
    void shouldThrowWhenChangingStatusOfUnknownTrip() {
        assertThrows(
                TripNotFoundException.class,
                () -> service.changeTripStatus(99, TripStatus.IN_PROGRESS)
        );
    }

    @Test
    void shouldFindTripsByTerrain() {
        Trip asphaltTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip offRoadTrip = createTrip(2, 200, TerrainType.OFF_ROAD, 2, yamaha);
        service.registerTrip(asphaltTrip);
        service.registerTrip(offRoadTrip);

        List<Trip> result = service.findTripsByTerrain(TerrainType.OFF_ROAD);

        assertEquals(1, result.size());
        assertSame(offRoadTrip, result.getFirst());
    }

    @Test
    void shouldFindPlannedTrips() {
        Trip plannedTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip inProgressTrip = createTrip(2, 200, TerrainType.MIXED, 2, yamaha);
        inProgressTrip.changeStatus(TripStatus.IN_PROGRESS);
        service.registerTrip(plannedTrip);
        service.registerTrip(inProgressTrip);

        List<Trip> result = service.findPlannedTrips();

        assertEquals(1, result.size());
        assertSame(plannedTrip, result.getFirst());
    }

    @Test
    void shouldCountTripsByStatus() {
        Trip plannedTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip completedTrip = createTrip(2, 200, TerrainType.MIXED, 2, yamaha);
        complete(completedTrip);
        service.registerTrip(plannedTrip);
        service.registerTrip(completedTrip);

        Map<TripStatus, Long> result = service.countTripsByStatus();

        assertEquals(1L, result.get(TripStatus.PLANNED));
        assertEquals(1L, result.get(TripStatus.COMPLETED));
    }

    @Test
    void shouldFindTripsByDate() {
        LocalDate searchedDate = LocalDate.now().plusDays(5);
        Trip tripOnDate = createTrip(1, 100, TerrainType.ASPHALT, searchedDate, honda);
        Trip tripOnOtherDate = createTrip(2, 200, TerrainType.MIXED, searchedDate.plusDays(1), yamaha);
        service.registerTrip(tripOnDate);
        service.registerTrip(tripOnOtherDate);

        List<Trip> result = service.findTripsByDate(searchedDate);

        assertEquals(1, result.size());
        assertSame(tripOnDate, result.getFirst());
    }

    @Test
    void shouldFindUpcomingTrips() {
        LocalDate today = LocalDate.now();
        Trip tripForToday = createTrip(1, 100, TerrainType.ASPHALT, today, honda);
        Trip futureTrip = createTrip(2, 200, TerrainType.MIXED, today.plusDays(2), yamaha);
        service.registerTrip(futureTrip);
        service.registerTrip(tripForToday);

        List<Trip> result = service.findUpcomingTrips();

        assertEquals(List.of(tripForToday, futureTrip), result);
        assertSame(tripForToday, result.getFirst());
    }

    @Test
    void shouldCountCompletedTrips() {
        Trip completedTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip plannedTrip = createTrip(2, 200, TerrainType.MIXED, 2, yamaha);
        complete(completedTrip);
        service.registerTrip(completedTrip);
        service.registerTrip(plannedTrip);

        assertEquals(1, service.countCompletedTrips());
    }

    @Test
    void shouldCalculateTotalCompletedDistance() {
        Trip firstCompletedTrip = createTrip(1, 100.5, TerrainType.ASPHALT, 1, honda);
        Trip secondCompletedTrip = createTrip(2, 149.5, TerrainType.MIXED, 2, yamaha);
        Trip plannedTrip = createTrip(3, 500, TerrainType.OFF_ROAD, 3, honda);
        complete(firstCompletedTrip);
        complete(secondCompletedTrip);
        service.registerTrip(firstCompletedTrip);
        service.registerTrip(secondCompletedTrip);
        service.registerTrip(plannedTrip);

        double result = service.calculateTotalCompletedDistance();

        assertEquals(250, result, 0.001);
    }

    @Test
    void shouldCountTripsByMotorcycle() {
        service.registerTrip(createTrip(1, 100, TerrainType.ASPHALT, 1, honda));
        service.registerTrip(createTrip(2, 200, TerrainType.MIXED, 2, honda));
        service.registerTrip(createTrip(3, 300, TerrainType.OFF_ROAD, 3, yamaha));

        Map<Motorcycle, Long> result = service.countTripsByMotorcycle();

        assertEquals(2L, result.get(honda));
        assertEquals(1L, result.get(yamaha));
    }

    @Test
    void shouldCalculateCompletedDistanceByMotorcycle() {
        Trip completedHondaTrip = createTrip(1, 100, TerrainType.ASPHALT, 1, honda);
        Trip plannedHondaTrip = createTrip(2, 200, TerrainType.MIXED, 2, honda);
        Trip completedYamahaTrip = createTrip(3, 300, TerrainType.OFF_ROAD, 3, yamaha);
        complete(completedHondaTrip);
        complete(completedYamahaTrip);
        service.registerTrip(completedHondaTrip);
        service.registerTrip(plannedHondaTrip);
        service.registerTrip(completedYamahaTrip);

        double result = service.calculateCompletedDistanceByMotorcycle(honda);

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
