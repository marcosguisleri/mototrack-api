package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.exception.TripDateUpdateRequiredException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.FuelEntryRepository;
import br.dev.guisleri.mototrack.repository.TripExpenseRepository;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final Long OWNER_ID = 1L;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private FuelEntryRepository fuelEntryRepository;

    @Mock
    private TripExpenseRepository tripExpenseRepository;

    private TripService service;
    private Motorcycle honda;
    private Motorcycle yamaha;
    private Motorcycle foreignMotorcycle;
    private Clock fixedClock;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                Instant.parse("2026-09-14T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        today = LocalDate.now(fixedClock);
        service = new TripService(
                tripRepository,
                fuelEntryRepository,
                tripExpenseRepository,
                fixedClock
        );
        User owner = User.restore(
                OWNER_ID,
                "Marcos",
                "marcos@example.com",
                "password-hash"
        );
        honda = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, owner
        );
        yamaha = Motorcycle.restore(
                2L, "Yamaha", "Tenere 700", "Blue", 2024, 689, MotorcycleType.NAKED, owner
        );
        User otherOwner = User.restore(
                2L,
                "Ana",
                "ana@example.com",
                "password-hash"
        );
        foreignMotorcycle = Motorcycle.restore(
                3L, "BMW", "F 900 GS", "White", 2025, 895, MotorcycleType.NAKED, otherOwner
        );
    }

    // -------------------------------------------------------------------------
    // Agendamento de viagens planejadas
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Registro de viagens já concluídas
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Consulta individual e controle de acesso
    // -------------------------------------------------------------------------

    @Test
    void shouldFindTripByIdForOwner() {
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        Trip result = service.findTripByIdForOwner(1L, OWNER_ID);

        assertSame(trip, result);
        verify(tripRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenFindingUnknownTripForOwner() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> service.findTripByIdForOwner(99L, OWNER_ID)
        );

        verify(tripRepository).findById(99L);
    }

    @Test
    void shouldDenyFindingTripOwnedByAnotherUser() {
        Trip trip = plannedTrip(1L, 1, foreignMotorcycle);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.findTripByIdForOwner(1L, OWNER_ID)
        );

        verify(tripRepository).findById(1L);
    }

    // -------------------------------------------------------------------------
    // Listagem geral por proprietário
    // -------------------------------------------------------------------------

    @Test
    void shouldFindTripsByOwner() {
        List<Trip> trips = List.of(
                plannedTrip(1L, 1, honda),
                plannedTrip(2L, 2, yamaha)
        );
        when(tripRepository.findByOwnerId(OWNER_ID)).thenReturn(trips);

        List<Trip> result = service.findTripsByOwnerId(OWNER_ID);

        assertSame(trips, result);
        verify(tripRepository).findByOwnerId(OWNER_ID);
    }

    // -------------------------------------------------------------------------
    // Mudança de status, confirmação de data, lock e persistência
    // -------------------------------------------------------------------------

    @Test
    void shouldStartTripOnScheduledDate() {
        Trip trip = plannedTrip(1L, 0, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        service.changeTripStatusForOwner(1L, OWNER_ID, TripStatus.IN_PROGRESS, false, null);

        assertAll(
                () -> assertEquals(TripStatus.IN_PROGRESS, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldUseCurrentInstantWhenOccurredAtIsNotInformed() {
        Trip trip = plannedTrip(1L, 0, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        service.changeTripStatusForOwner(1L, OWNER_ID, TripStatus.IN_PROGRESS, false, null);

        assertEquals(fixedClock.instant(), trip.getStartedAt());
    }

    @Test
    void shouldUseInformedOccurredAtWhenStartingAndCompletingTrip() {
        Trip trip = plannedTrip(1L, 0, honda);
        Instant startedAt = fixedClock.instant().minusSeconds(7200);
        Instant completedAt = fixedClock.instant().minusSeconds(600);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        service.changeTripStatusForOwner(
                1L, OWNER_ID, TripStatus.IN_PROGRESS, false, startedAt
        );
        service.changeTripStatusForOwner(
                1L, OWNER_ID, TripStatus.COMPLETED, false, completedAt
        );

        assertAll(
                () -> assertEquals(startedAt, trip.getStartedAt()),
                () -> assertEquals(completedAt, trip.getCompletedAt())
        );
    }

    @Test
    void shouldRejectOccurredAtInTheFutureWithoutSaving() {
        Trip trip = plannedTrip(1L, 0, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                InvalidTripDateException.class,
                () -> service.changeTripStatusForOwner(
                        1L,
                        OWNER_ID,
                        TripStatus.IN_PROGRESS,
                        false,
                        fixedClock.instant().plusSeconds(1)
                )
        );
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldStartEarlyTripAndUpdateDateWhenConfirmed() {
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        service.changeTripStatusForOwner(
                1L,
                OWNER_ID,
                TripStatus.IN_PROGRESS,
                true,
                null
        );

        assertAll(
                () -> assertEquals(TripStatus.IN_PROGRESS, trip.getStatus()),
                () -> assertEquals(today, trip.getTripDate())
        );
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldRejectEarlyStartWithoutDateUpdateConfirmation() {
        LocalDate originalDate = today.plusDays(1);
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                TripDateUpdateRequiredException.class,
                () -> service.changeTripStatusForOwner(
                        1L,
                        OWNER_ID,
                        TripStatus.IN_PROGRESS,
                        false,
                        null
                )
        );

        assertAll(
                () -> assertEquals(TripStatus.PLANNED, trip.getStatus()),
                () -> assertEquals(originalDate, trip.getTripDate())
        );
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldCompleteTripInProgress() {
        Trip trip = trip(
                1L,
                TerrainType.ASPHALT,
                today,
                honda,
                TripStatus.IN_PROGRESS
        );
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        service.changeTripStatusForOwner(
                1L,
                OWNER_ID,
                TripStatus.COMPLETED,
                false,
                null
        );

        assertEquals(TripStatus.COMPLETED, trip.getStatus());
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldRejectChangingTripStatusToPlanned() {
        Trip trip = plannedTrip(1L, 0, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                InvalidTripStatusException.class,
                () -> service.changeTripStatusForOwner(
                        1L,
                        OWNER_ID,
                        TripStatus.PLANNED,
                        false,
                        null
                )
        );

        assertEquals(TripStatus.PLANNED, trip.getStatus());
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldRejectCompletingPlannedTrip() {
        Trip trip = plannedTrip(1L, 0, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                InvalidTripStatusException.class,
                () -> service.changeTripStatusForOwner(
                        1L,
                        OWNER_ID,
                        TripStatus.COMPLETED,
                        false,
                        null
                )
        );

        assertEquals(TripStatus.PLANNED, trip.getStatus());
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenChangingStatusOfUnknownTripForOwner() {
        when(tripRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> service.changeTripStatusForOwner(
                        99L,
                        OWNER_ID,
                        TripStatus.IN_PROGRESS,
                        false,
                        null
                )
        );

        verify(tripRepository).findByIdForUpdate(99L);
        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void shouldDenyChangingStatusOfTripOwnedByAnotherUser() {
        Trip trip = plannedTrip(1L, 1, foreignMotorcycle);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.changeTripStatusForOwner(
                        1L,
                        OWNER_ID,
                        TripStatus.IN_PROGRESS,
                        false,
                        null
                )
        );

        assertEquals(TripStatus.PLANNED, trip.getStatus());
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void shouldRequireConfirmationToCompleteFutureTrip() {
        Trip trip = trip(
                1L,
                TerrainType.ASPHALT,
                today.plusDays(10),
                honda,
                TripStatus.IN_PROGRESS
        );
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                TripDateUpdateRequiredException.class,
                () -> service.changeTripStatusForOwner(
                        1L,
                        OWNER_ID,
                        TripStatus.COMPLETED,
                        false,
                        null
                )
        );
        assertEquals(TripStatus.IN_PROGRESS, trip.getStatus());
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldCompleteFutureTripAndUpdateDateWhenConfirmed() {
        Trip trip = trip(
                1L,
                TerrainType.ASPHALT,
                today.plusDays(10),
                honda,
                TripStatus.IN_PROGRESS
        );
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        service.changeTripStatusForOwner(
                1L,
                OWNER_ID,
                TripStatus.COMPLETED,
                true,
                null
        );

        assertEquals(TripStatus.COMPLETED, trip.getStatus());
        assertEquals(today, trip.getTripDate());
        verify(tripRepository).save(trip);
    }

    // -------------------------------------------------------------------------
    // Edição de viagem
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateTripUnderLockAndSave() {
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        Trip result = service.updateTripForOwner(
                1L, OWNER_ID, "Campinas", "Santos", 170.0,
                TerrainType.MIXED, today.plusDays(3), yamaha
        );

        assertAll(
                () -> assertSame(trip, result),
                () -> assertEquals("Campinas", trip.getOrigin()),
                () -> assertEquals(today.plusDays(3), trip.getTripDate()),
                () -> assertSame(yamaha, trip.getMotorcycle())
        );
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldRejectInvalidDateOnUpdateWithoutSaving() {
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                InvalidTripDateException.class,
                () -> service.updateTripForOwner(
                        1L, OWNER_ID, "Campinas", "Santos", 170.0,
                        TerrainType.MIXED, today.minusDays(1), honda
                )
        );
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldNotUpdateTripOfAnotherOwner() {
        Trip trip = trip(1L, TerrainType.ASPHALT, today.plusDays(1), foreignMotorcycle, TripStatus.PLANNED);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.updateTripForOwner(
                        1L, OWNER_ID, "Campinas", "Santos", 170.0,
                        TerrainType.MIXED, today.plusDays(3), honda
                )
        );
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldReportMissingTripOnUpdate() {
        when(tripRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> service.updateTripForOwner(
                        99L, OWNER_ID, "Campinas", "Santos", 170.0,
                        TerrainType.MIXED, today.plusDays(3), honda
                )
        );
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldCorrectTimestampsUnderLockAndSave() {
        Trip trip = trip(1L, TerrainType.ASPHALT, today, honda, TripStatus.COMPLETED);
        Instant startedAt = fixedClock.instant().minusSeconds(7200);
        Instant completedAt = fixedClock.instant().minusSeconds(600);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        Trip result = service.correctTripTimestampsForOwner(
                1L, OWNER_ID, startedAt, completedAt
        );

        assertAll(
                () -> assertSame(trip, result),
                () -> assertEquals(startedAt, trip.getStartedAt()),
                () -> assertEquals(completedAt, trip.getCompletedAt())
        );
        verify(tripRepository).findByIdForUpdate(1L);
        verify(tripRepository).save(trip);
    }

    @Test
    void shouldRejectFutureTimestampOnCorrectionWithoutSaving() {
        Trip trip = trip(1L, TerrainType.ASPHALT, today, honda, TripStatus.COMPLETED);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                InvalidTripDateException.class,
                () -> service.correctTripTimestampsForOwner(
                        1L, OWNER_ID, null, fixedClock.instant().plusSeconds(1)
                )
        );
        verify(tripRepository, never()).save(any());
    }

    @Test
    void shouldNotCorrectTimestampsOfTripOfAnotherOwner() {
        Trip trip = trip(1L, TerrainType.ASPHALT, today, foreignMotorcycle, TripStatus.COMPLETED);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.correctTripTimestampsForOwner(1L, OWNER_ID, null, null)
        );
        verify(tripRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Filtros, viagem em andamento, próximas viagens e histórico
    // -------------------------------------------------------------------------

    @Test
    void shouldFindTripsByOwnerAndTerrain() {
        List<Trip> trips = List.of(plannedTrip(1L, 1, honda));
        when(tripRepository.findByOwnerIdAndTerrain(OWNER_ID, TerrainType.OFF_ROAD))
                .thenReturn(trips);

        List<Trip> result = service.findTripsByOwnerIdAndTerrain(
                OWNER_ID,
                TerrainType.OFF_ROAD
        );

        assertSame(trips, result);
        verify(tripRepository).findByOwnerIdAndTerrain(OWNER_ID, TerrainType.OFF_ROAD);
    }

    @Test
    void shouldFindTripsByOwnerAndStatus() {
        List<Trip> trips = List.of(plannedTrip(1L, 1, honda));
        when(tripRepository.findByOwnerIdAndStatus(OWNER_ID, TripStatus.PLANNED))
                .thenReturn(trips);

        List<Trip> result = service.findTripsByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.PLANNED
        );

        assertSame(trips, result);
        verify(tripRepository).findByOwnerIdAndStatus(OWNER_ID, TripStatus.PLANNED);
    }

    @Test
    void shouldFindFirstInProgressTripByOwner() {
        Trip firstTrip = trip(
                1L,
                TerrainType.ASPHALT,
                today,
                honda,
                TripStatus.IN_PROGRESS
        );
        Trip secondTrip = trip(
                2L,
                TerrainType.MIXED,
                today,
                yamaha,
                TripStatus.IN_PROGRESS
        );
        when(tripRepository.findByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.IN_PROGRESS
        )).thenReturn(List.of(firstTrip, secondTrip));

        Optional<Trip> result = service.findInProgressTripByOwnerId(OWNER_ID);

        assertSame(firstTrip, result.orElseThrow());
        verify(tripRepository).findByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.IN_PROGRESS
        );
    }

    @Test
    void shouldReturnEmptyWhenOwnerHasNoInProgressTrip() {
        when(tripRepository.findByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.IN_PROGRESS
        )).thenReturn(List.of());

        Optional<Trip> result = service.findInProgressTripByOwnerId(OWNER_ID);

        assertEquals(Optional.empty(), result);
        verify(tripRepository).findByOwnerIdAndStatus(
                OWNER_ID,
                TripStatus.IN_PROGRESS
        );
    }

    @Test
    void shouldFindTripsByOwnerAndDate() {
        LocalDate searchedDate = today.plusDays(5);
        List<Trip> trips = List.of(
                trip(1L, TerrainType.ASPHALT, searchedDate, honda, TripStatus.PLANNED)
        );
        when(tripRepository.findByOwnerIdAndTripDate(OWNER_ID, searchedDate))
                .thenReturn(trips);

        List<Trip> result = service.findTripsByOwnerIdAndDate(OWNER_ID, searchedDate);

        assertSame(trips, result);
        verify(tripRepository).findByOwnerIdAndTripDate(OWNER_ID, searchedDate);
    }

    @Test
    void shouldFindUpcomingTripsByOwnerUsingClock() {
        List<Trip> trips = List.of(
                plannedTrip(1L, 0, honda),
                plannedTrip(2L, 2, yamaha)
        );
        when(tripRepository.findUpcomingByOwnerIdFromDate(OWNER_ID, today))
                .thenReturn(trips);

        List<Trip> result = service.findUpcomingTripsByOwnerId(OWNER_ID);

        assertSame(trips, result);
        verify(tripRepository).findUpcomingByOwnerIdFromDate(OWNER_ID, today);
    }

    @Test
    void shouldFindNextUpcomingTripByOwnerUsingClock() {
        Trip nextTrip = plannedTrip(1L, 2, honda);
        when(tripRepository.findFirstUpcomingByOwnerIdFromDate(OWNER_ID, today))
                .thenReturn(Optional.of(nextTrip));

        Optional<Trip> result = service.findNextUpcomingTripByOwnerId(OWNER_ID);

        assertSame(nextTrip, result.orElseThrow());
        verify(tripRepository)
                .findFirstUpcomingByOwnerIdFromDate(OWNER_ID, today);
    }

    @Test
    void shouldFindCompletedTripsByOwnerId() {
        List<Trip> completedTrips = List.of(
                trip(
                        1L,
                        TerrainType.ASPHALT,
                        LocalDate.of(2026, 9, 20),
                        honda,
                        TripStatus.COMPLETED
                ),
                trip(
                        2L,
                        TerrainType.MIXED,
                        LocalDate.of(2026, 8, 15),
                        yamaha,
                        TripStatus.COMPLETED
                )
        );
        when(tripRepository.findCompletedByOwnerIdOrderByTripDateDesc(OWNER_ID))
                .thenReturn(completedTrips);

        List<Trip> result = service.findCompletedTripsByOwnerId(OWNER_ID);

        assertSame(completedTrips, result);
        verify(tripRepository)
                .findCompletedByOwnerIdOrderByTripDateDesc(OWNER_ID);
    }

    // -------------------------------------------------------------------------
    // Cálculo de dias restantes e controle de acesso
    // -------------------------------------------------------------------------

    @Test
    void shouldCalculateDaysUntilPlannedTripForOwner() {
        Trip trip = plannedTrip(1L, 12, honda);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        long result = service.calculateDaysUntilTripForOwner(1L, OWNER_ID);

        assertEquals(12, result);
        verify(tripRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenCalculatingDaysUntilCompletedTripForOwner() {
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
                () -> service.calculateDaysUntilTripForOwner(1L, OWNER_ID)
        );

        verify(tripRepository).findById(1L);
    }

    @Test
    void shouldDenyCalculatingDaysUntilTripOwnedByAnotherUser() {
        Trip trip = plannedTrip(1L, 12, foreignMotorcycle);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.calculateDaysUntilTripForOwner(1L, OWNER_ID)
        );

        verify(tripRepository).findById(1L);
    }

    // -------------------------------------------------------------------------
    // Exclusão com lock e controle de acesso
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteTripByIdForOwner() {
        Trip trip = plannedTrip(1L, 1, honda);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        service.deleteTripByIdForOwner(1L, OWNER_ID);

        verify(tripRepository).findByIdForUpdate(1L);
        verify(fuelEntryRepository).deleteByTripId(1L);
        verify(tripExpenseRepository).deleteByTripId(1L);
        verify(tripRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingUnknownTripForOwner() {
        when(tripRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> service.deleteTripByIdForOwner(99L, OWNER_ID)
        );

        verify(tripRepository).findByIdForUpdate(99L);
        verify(fuelEntryRepository, never()).deleteByTripId(99L);
        verify(tripExpenseRepository, never()).deleteByTripId(99L);
        verify(tripRepository, never()).deleteById(99L);
    }

    @Test
    void shouldDenyDeletingTripOwnedByAnotherUser() {
        Trip trip = plannedTrip(1L, 1, foreignMotorcycle);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.deleteTripByIdForOwner(1L, OWNER_ID)
        );

        verify(tripRepository).findByIdForUpdate(1L);
        verify(fuelEntryRepository, never()).deleteByTripId(1L);
        verify(tripExpenseRepository, never()).deleteByTripId(1L);
        verify(tripRepository, never()).deleteById(1L);
    }

    // -------------------------------------------------------------------------
    // Helpers de domínio
    // -------------------------------------------------------------------------

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
                status,
                null,
                null
        );
    }
}
