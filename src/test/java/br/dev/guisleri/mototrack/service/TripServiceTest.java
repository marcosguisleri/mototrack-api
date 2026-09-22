package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    private TripService service;
    private Motorcycle honda;
    private Motorcycle yamaha;
    private Clock fixedClock;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                Instant.parse("2026-09-14T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        today = LocalDate.now(fixedClock);
        service = new TripService(tripRepository, fixedClock);
        User owner = User.restore(1L, "Marcos", "marcos@example.com");
        honda = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, owner
        );
        yamaha = Motorcycle.restore(
                2L, "Yamaha", "Tenere 700", "Blue", 2024, 689, owner
        );
    }

    @Test
    void shouldScheduleTrip() {
        Trip persistedTrip = trip(
                1L,
                TerrainType.ASPHALT,
                today.plusDays(1),
                honda,
                TripStatus.PLANNED
        );
        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        when(tripRepository.save(any(Trip.class))).thenReturn(persistedTrip);

        Trip result = service.scheduleTrip(
                "Origem 1",
                "Destino 1",
                100,
                TerrainType.ASPHALT,
                today.plusDays(1),
                honda
        );

        verify(tripRepository).save(tripCaptor.capture());
        Trip tripSentToRepository = tripCaptor.getValue();
        assertNull(tripSentToRepository.getId());
        assertEquals(TripStatus.PLANNED, tripSentToRepository.getStatus());
        assertEquals(today.plusDays(1), tripSentToRepository.getTripDate());
        assertSame(honda, tripSentToRepository.getMotorcycle());
        assertSame(persistedTrip, result);
    }

    @Test
    void shouldRegisterCompletedTrip() {
        Trip persistedTrip = trip(
                1L,
                TerrainType.MIXED,
                today.minusDays(1),
                honda,
                TripStatus.COMPLETED
        );
        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        when(tripRepository.save(any(Trip.class))).thenReturn(persistedTrip);

        Trip result = service.registerCompletedTrip(
                "Florianopolis",
                "Urubici",
                175.5,
                TerrainType.MIXED,
                today.minusDays(1),
                honda
        );

        verify(tripRepository).save(tripCaptor.capture());
        assertNull(tripCaptor.getValue().getId());
        assertEquals(TripStatus.COMPLETED, tripCaptor.getValue().getStatus());
        assertSame(persistedTrip, result);
    }

    @Test
    void shouldFindTripById() {
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        Trip result = service.findTripById(1L);

        assertSame(trip, result);
        verify(tripRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenFindingUnknownTrip() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> service.findTripById(99L)
        );

        verify(tripRepository).findById(99L);
    }

    @Test
    void shouldFindAllTrips() {
        List<Trip> trips = List.of(
                plannedTrip(1L, 1, honda),
                plannedTrip(2L, 2, yamaha)
        );
        when(tripRepository.findAll()).thenReturn(trips);

        List<Trip> result = service.findAllTrips();

        assertSame(trips, result);
        verify(tripRepository).findAll();
    }

    @Test
    void shouldChangeTripStatus() {
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        service.changeTripStatus(1L, TripStatus.IN_PROGRESS);

        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
        verify(tripRepository).findById(1L);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldThrowWhenChangingStatusOfUnknownTrip() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> service.changeTripStatus(99L, TripStatus.IN_PROGRESS)
        );

        verify(tripRepository).findById(99L);
        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void shouldFindTripsByTerrain() {
        List<Trip> trips = List.of(plannedTrip(1L, 1, honda));
        when(tripRepository.findByTerrain(TerrainType.OFF_ROAD)).thenReturn(trips);

        List<Trip> result = service.findTripsByTerrain(TerrainType.OFF_ROAD);

        assertSame(trips, result);
        verify(tripRepository).findByTerrain(TerrainType.OFF_ROAD);
    }

    @Test
    void shouldFindTripsByStatus() {
        List<Trip> trips = List.of(plannedTrip(1L, 1, honda));
        when(tripRepository.findByStatus(TripStatus.PLANNED)).thenReturn(trips);

        List<Trip> result = service.findTripsByStatus(TripStatus.PLANNED);

        assertSame(trips, result);
        verify(tripRepository).findByStatus(TripStatus.PLANNED);
    }

    @Test
    void shouldFindTripsByDate() {
        LocalDate searchedDate = today.plusDays(5);
        List<Trip> trips = List.of(
                trip(1L, TerrainType.ASPHALT, searchedDate, honda, TripStatus.PLANNED)
        );
        when(tripRepository.findByTripDate(searchedDate)).thenReturn(trips);

        List<Trip> result = service.findTripsByDate(searchedDate);

        assertSame(trips, result);
        verify(tripRepository).findByTripDate(searchedDate);
    }

    @Test
    void shouldFindUpcomingTrips() {
        List<Trip> trips = List.of(
                plannedTrip(1L, 0, honda),
                plannedTrip(2L, 2, yamaha)
        );
        when(tripRepository.findUpcomingFrom(today)).thenReturn(trips);

        List<Trip> result = service.findUpcomingTrips();

        assertSame(trips, result);
        verify(tripRepository).findUpcomingFrom(today);
    }

    @Test
    void shouldCalculateDaysUntilPlannedTrip() {
        Trip trip = plannedTrip(1L, 12, honda);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        long result = service.calculateDaysUntilTrip(1L);

        assertEquals(12, result);
        verify(tripRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenCalculatingDaysUntilCompletedTrip() {
        Trip trip = trip(
                1L,
                TerrainType.ASPHALT,
                today.minusDays(1),
                honda,
                TripStatus.COMPLETED
        );
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                InvalidTripStatusException.class,
                () -> service.calculateDaysUntilTrip(1L)
        );

        verify(tripRepository).findById(1L);
    }

    @Test
    void shouldRejectCompletingFutureTrip() {
        Trip trip = plannedTrip(1L, 10, honda);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        service.changeTripStatus(1L, TripStatus.IN_PROGRESS);

        assertThrows(
                InvalidTripDateException.class,
                () -> service.changeTripStatus(1L, TripStatus.COMPLETED)
        );
        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
        verify(tripRepository, times(2)).findById(1L);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldDeleteTripById() {
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        service.deleteTripById(1L);

        verify(tripRepository).findById(1L);
        verify(tripRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingUnknownTrip() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> service.deleteTripById(99L)
        );

        verify(tripRepository).findById(99L);
        verify(tripRepository, never()).deleteById(99L);
    }

    private Trip plannedTrip(
            Long id,
            int daysFromToday,
            Motorcycle motorcycle
    ) {
        return trip(
                id,
                TerrainType.ASPHALT,
                today.plusDays(daysFromToday),
                motorcycle,
                TripStatus.PLANNED
        );
    }

    private Trip trip(
            Long id,
            TerrainType terrainType,
            LocalDate tripDate,
            Motorcycle motorcycle,
            TripStatus status
    ) {
        return Trip.restore(
                id,
                "Origem " + id,
                "Destino " + id,
                100,
                terrainType,
                tripDate,
                motorcycle,
                status
        );
    }
}
