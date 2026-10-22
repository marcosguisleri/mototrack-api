package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.FuelEntryNotFoundException;
import br.dev.guisleri.mototrack.exception.InvalidFuelEntryException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.model.FuelEntry;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.FuelEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelEntryServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final long TRIP_ID = 5L;
    private static final long ENTRY_ID = 9L;

    @Mock
    private FuelEntryRepository fuelEntryRepository;

    @Mock
    private TripService tripService;

    private FuelEntryService service;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                Instant.parse("2026-09-14T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );
        service = new FuelEntryService(fuelEntryRepository, tripService, fixedClock);
    }

    // -------------------------------------------------------------------------
    // Registro de abastecimento
    // -------------------------------------------------------------------------

    @Test
    void shouldRegisterFuelEntryOnTripInProgress() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.IN_PROGRESS));
        FuelEntry persisted = fuelEntry(ENTRY_ID);
        when(fuelEntryRepository.save(any(FuelEntry.class))).thenReturn(persisted);
        ArgumentCaptor<FuelEntry> captor = ArgumentCaptor.forClass(FuelEntry.class);

        FuelEntry result = service.registerFuelEntryForOwner(
                TRIP_ID, OWNER_ID, new BigDecimal("10.2"), new BigDecimal("64.90")
        );

        verify(fuelEntryRepository).save(captor.capture());
        assertAll(
                () -> assertSame(persisted, result),
                () -> assertEquals(TRIP_ID, captor.getValue().getTripId()),
                () -> assertEquals(new BigDecimal("10.2"), captor.getValue().getLiters()),
                () -> assertEquals(fixedClock.instant(), captor.getValue().getCreatedAt())
        );
    }

    @Test
    void shouldRegisterFuelEntryOnCompletedTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(fuelEntryRepository.save(any(FuelEntry.class)))
                .thenReturn(fuelEntry(ENTRY_ID));

        service.registerFuelEntryForOwner(
                TRIP_ID, OWNER_ID, new BigDecimal("10"), new BigDecimal("60")
        );

        verify(fuelEntryRepository).save(any(FuelEntry.class));
    }

    @Test
    void shouldRejectFuelEntryOnPlannedTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.PLANNED));

        assertThrows(
                InvalidTripStatusException.class,
                () -> service.registerFuelEntryForOwner(
                        TRIP_ID, OWNER_ID, new BigDecimal("10"), new BigDecimal("60")
                )
        );
        verify(fuelEntryRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidFuelEntryValuesWithoutSaving() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.IN_PROGRESS));

        assertThrows(
                InvalidFuelEntryException.class,
                () -> service.registerFuelEntryForOwner(
                        TRIP_ID, OWNER_ID, BigDecimal.ZERO, new BigDecimal("60")
                )
        );
        verify(fuelEntryRepository, never()).save(any());
    }

    @Test
    void shouldPropagateAccessDeniedWhenRegisteringOnForeignTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenThrow(new TripAccessDeniedException("negado"));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.registerFuelEntryForOwner(
                        TRIP_ID, OWNER_ID, new BigDecimal("10"), new BigDecimal("60")
                )
        );
        verify(fuelEntryRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Listagem
    // -------------------------------------------------------------------------

    @Test
    void shouldFindFuelEntriesOfOwnedTrip() {
        List<FuelEntry> entries = List.of(fuelEntry(1L), fuelEntry(2L));
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.PLANNED));
        when(fuelEntryRepository.findByTripId(TRIP_ID)).thenReturn(entries);

        assertSame(entries, service.findFuelEntriesByTripIdForOwner(TRIP_ID, OWNER_ID));
    }

    @Test
    void shouldNotListFuelEntriesOfForeignTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenThrow(new TripAccessDeniedException("negado"));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.findFuelEntriesByTripIdForOwner(TRIP_ID, OWNER_ID)
        );
        verify(fuelEntryRepository, never()).findByTripId(TRIP_ID);
    }

    // -------------------------------------------------------------------------
    // Edição e exclusão
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateFuelEntryUnderLock() {
        FuelEntry existing = fuelEntry(ENTRY_ID);
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(fuelEntryRepository.findByIdAndTripIdForUpdate(ENTRY_ID, TRIP_ID))
                .thenReturn(Optional.of(existing));
        when(fuelEntryRepository.save(existing)).thenReturn(existing);

        FuelEntry result = service.updateFuelEntryForOwner(
                TRIP_ID, ENTRY_ID, OWNER_ID, new BigDecimal("12"), new BigDecimal("63.10")
        );

        assertAll(
                () -> assertSame(existing, result),
                () -> assertEquals(new BigDecimal("12"), existing.getLiters()),
                () -> assertEquals(new BigDecimal("63.10"), existing.getTotalCost())
        );
        verify(fuelEntryRepository).save(existing);
    }

    @Test
    void shouldReportMissingFuelEntryWhenUpdating() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(fuelEntryRepository.findByIdAndTripIdForUpdate(ENTRY_ID, TRIP_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                FuelEntryNotFoundException.class,
                () -> service.updateFuelEntryForOwner(
                        TRIP_ID, ENTRY_ID, OWNER_ID, new BigDecimal("12"), new BigDecimal("60")
                )
        );
        verify(fuelEntryRepository, never()).save(any());
    }

    @Test
    void shouldDeleteFuelEntryUnderLock() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(fuelEntryRepository.findByIdAndTripIdForUpdate(ENTRY_ID, TRIP_ID))
                .thenReturn(Optional.of(fuelEntry(ENTRY_ID)));

        service.deleteFuelEntryForOwner(TRIP_ID, ENTRY_ID, OWNER_ID);

        verify(fuelEntryRepository).deleteById(ENTRY_ID);
    }

    @Test
    void shouldNotDeleteFuelEntryFromAnotherTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED));
        when(fuelEntryRepository.findByIdAndTripIdForUpdate(ENTRY_ID, TRIP_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                FuelEntryNotFoundException.class,
                () -> service.deleteFuelEntryForOwner(TRIP_ID, ENTRY_ID, OWNER_ID)
        );
        verify(fuelEntryRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Trip trip(TripStatus status) {
        User owner = User.restore(OWNER_ID, "Marcos", "marcos@example.com", "hash");
        Motorcycle motorcycle = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, owner
        );
        return Trip.restore(
                TRIP_ID,
                "Campinas",
                "Santos",
                170.0,
                TerrainType.ASPHALT,
                LocalDate.of(2026, 9, 14),
                motorcycle,
                status,
                null,
                null
        );
    }

    private FuelEntry fuelEntry(long id) {
        return FuelEntry.restore(
                id,
                TRIP_ID,
                new BigDecimal("10.2"),
                new BigDecimal("64.90"),
                fixedClock.instant()
        );
    }
}
