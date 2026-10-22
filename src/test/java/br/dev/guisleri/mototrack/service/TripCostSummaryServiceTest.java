package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.ExpenseCategorySummary;
import br.dev.guisleri.mototrack.model.FuelCostSummary;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripCostSummary;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.TripCostSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripCostSummaryServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final long TRIP_ID = 5L;

    @Mock
    private TripCostSummaryRepository tripCostSummaryRepository;

    @Mock
    private TripService tripService;

    private TripCostSummaryService service;

    @BeforeEach
    void setUp() {
        service = new TripCostSummaryService(tripCostSummaryRepository, tripService);
    }

    @Test
    void shouldBuildSummaryFromRepositoryAndTripDistance() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.COMPLETED, 100.0));
        when(tripCostSummaryRepository.summarizeFuelByTripId(TRIP_ID))
                .thenReturn(new FuelCostSummary(
                        1, new BigDecimal("10"), new BigDecimal("60.00")
                ));
        when(tripCostSummaryRepository.summarizeExpensesByCategoryForTripId(TRIP_ID))
                .thenReturn(List.of(new ExpenseCategorySummary(
                        ExpenseCategory.TOLL, 2, new BigDecimal("40.00")
                )));

        TripCostSummary summary = service.getCostSummaryForOwner(TRIP_ID, OWNER_ID);

        assertAll(
                () -> assertEquals(new BigDecimal("100.00"), summary.totalCost()),
                () -> assertEquals(new BigDecimal("1.00"), summary.costPerKm()),
                () -> assertEquals(1, summary.expenses().size())
        );
    }

    @Test
    void shouldReturnEmptySummaryForPlannedTripWithoutRecords() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenReturn(trip(TripStatus.PLANNED, 100.0));
        when(tripCostSummaryRepository.summarizeFuelByTripId(TRIP_ID))
                .thenReturn(new FuelCostSummary(0, null, null));
        when(tripCostSummaryRepository.summarizeExpensesByCategoryForTripId(TRIP_ID))
                .thenReturn(List.of());

        TripCostSummary summary = service.getCostSummaryForOwner(TRIP_ID, OWNER_ID);

        assertAll(
                () -> assertNull(summary.totalCost()),
                () -> assertNull(summary.costPerKm())
        );
    }

    @Test
    void shouldNotQueryTotalsForForeignTrip() {
        when(tripService.findTripByIdForOwner(TRIP_ID, OWNER_ID))
                .thenThrow(new TripAccessDeniedException("negado"));

        assertThrows(
                TripAccessDeniedException.class,
                () -> service.getCostSummaryForOwner(TRIP_ID, OWNER_ID)
        );
        verify(tripCostSummaryRepository, never()).summarizeFuelByTripId(TRIP_ID);
    }

    private Trip trip(TripStatus status, double distanceKm) {
        User owner = User.restore(OWNER_ID, "Marcos", "marcos@example.com", "hash");
        Motorcycle motorcycle = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, owner
        );
        return Trip.restore(
                TRIP_ID, "Campinas", "Santos", distanceKm, TerrainType.ASPHALT,
                LocalDate.of(2026, 9, 14), motorcycle, status, null, null
        );
    }
}
