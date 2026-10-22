package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.ExpenseCategorySummary;
import br.dev.guisleri.mototrack.model.FuelCostSummary;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.FuelEntryEntity;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripExpenseEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(JpaTripCostSummaryRepositoryAdapter.class)
class JpaTripCostSummaryRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired
    private JpaTripCostSummaryRepositoryAdapter adapter;

    @PersistenceContext
    private EntityManager entityManager;

    private TripEntity trip;
    private TripEntity otherTrip;
    private TripEntity emptyTrip;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity(null, "Marcos", "marcos@example.com", "hash");
        entityManager.persist(owner);
        MotorcycleEntity motorcycle = new MotorcycleEntity(
                null, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, owner
        );
        entityManager.persist(motorcycle);
        trip = persistTrip(motorcycle);
        otherTrip = persistTrip(motorcycle);
        emptyTrip = persistTrip(motorcycle);
    }

    @Test
    void shouldSummarizeFuelOnlyForTheGivenTrip() {
        persistFuel(trip, "10.2", "64.90");
        persistFuel(trip, "6", "39.00");
        persistFuel(otherTrip, "50", "300.00");
        flushAndClear();

        FuelCostSummary summary = adapter.summarizeFuelByTripId(trip.getId());

        assertAll(
                () -> assertEquals(2, summary.entryCount()),
                () -> assertEquals(0, new BigDecimal("16.2").compareTo(summary.liters())),
                () -> assertEquals(0, new BigDecimal("103.90").compareTo(summary.totalCost()))
        );
    }

    @Test
    void shouldReturnNullFuelTotalsWhenThereAreNoEntries() {
        persistFuel(otherTrip, "50", "300.00");
        flushAndClear();

        FuelCostSummary summary = adapter.summarizeFuelByTripId(emptyTrip.getId());

        assertAll(
                () -> assertEquals(0, summary.entryCount()),
                () -> assertNull(summary.liters()),
                () -> assertNull(summary.totalCost())
        );
    }

    @Test
    void shouldGroupExpensesByCategoryInEnumOrder() {
        persistExpense(trip, ExpenseCategory.FOOD, "27.01");
        persistExpense(trip, ExpenseCategory.TOLL, "13.90");
        persistExpense(trip, ExpenseCategory.TOLL, "23.90");
        persistExpense(otherTrip, ExpenseCategory.TOLL, "99.00");
        flushAndClear();

        List<ExpenseCategorySummary> summaries =
                adapter.summarizeExpensesByCategoryForTripId(trip.getId());

        assertAll(
                () -> assertEquals(2, summaries.size()),
                () -> assertEquals(ExpenseCategory.TOLL, summaries.get(0).category()),
                () -> assertEquals(2, summaries.get(0).count()),
                () -> assertEquals(0, new BigDecimal("37.80").compareTo(summaries.get(0).total())),
                () -> assertEquals(ExpenseCategory.FOOD, summaries.get(1).category()),
                () -> assertEquals(1, summaries.get(1).count()),
                () -> assertEquals(0, new BigDecimal("27.01").compareTo(summaries.get(1).total()))
        );
    }

    @Test
    void shouldCountExemptTollsWithZeroTotal() {
        persistExpense(trip, ExpenseCategory.TOLL, "0.00");
        persistExpense(trip, ExpenseCategory.TOLL, "0.00");
        flushAndClear();

        List<ExpenseCategorySummary> summaries =
                adapter.summarizeExpensesByCategoryForTripId(trip.getId());

        assertAll(
                () -> assertEquals(1, summaries.size()),
                () -> assertEquals(ExpenseCategory.TOLL, summaries.get(0).category()),
                () -> assertEquals(2, summaries.get(0).count()),
                () -> assertEquals(0, BigDecimal.ZERO.compareTo(summaries.get(0).total()))
        );
    }

    @Test
    void shouldReturnNoCategoriesWhenThereAreNoExpenses() {
        persistExpense(otherTrip, ExpenseCategory.TOLL, "99.00");
        flushAndClear();

        assertTrue(adapter.summarizeExpensesByCategoryForTripId(emptyTrip.getId()).isEmpty());
    }

    private TripEntity persistTrip(MotorcycleEntity motorcycle) {
        TripEntity tripEntity = new TripEntity(
                null, "Campinas", "Santos", 170.0, TripStatus.COMPLETED,
                TerrainType.ASPHALT, LocalDate.of(2026, 9, 14), motorcycle, null, null
        );
        entityManager.persist(tripEntity);
        return tripEntity;
    }

    private void persistFuel(TripEntity tripEntity, String liters, String cost) {
        entityManager.persist(new FuelEntryEntity(
                null, tripEntity, new BigDecimal(liters), new BigDecimal(cost), NOW
        ));
    }

    private void persistExpense(TripEntity tripEntity, ExpenseCategory category, String amount) {
        entityManager.persist(new TripExpenseEntity(
                null, tripEntity, category, new BigDecimal(amount), null, NOW
        ));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
