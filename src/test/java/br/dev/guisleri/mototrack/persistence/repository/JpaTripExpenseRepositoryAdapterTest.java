package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.ExpenseCategory;
import br.dev.guisleri.mototrack.model.TripExpense;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.TripExpenseMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        JpaTripExpenseRepositoryAdapter.class,
        TripExpenseMapper.class
})
class JpaTripExpenseRepositoryAdapterTest {

    private static final Instant BASE = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired
    private JpaTripExpenseRepositoryAdapter adapter;

    @Autowired
    private SpringDataTripExpenseRepository springDataTripExpenseRepository;

    @Autowired
    private SpringDataTripRepository springDataTripRepository;

    @Autowired
    private SpringDataMotorcycleRepository springDataMotorcycleRepository;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long tripId;
    private Long otherTripId;

    @BeforeEach
    void setUp() {
        UserEntity owner = springDataUserRepository.saveAndFlush(
                new UserEntity(null, "Marcos", "marcos@example.com", "hash")
        );
        MotorcycleEntity motorcycle = springDataMotorcycleRepository.saveAndFlush(
                new MotorcycleEntity(
                        null, "Honda", "NX 500", "Black", 2025, 471,
                        MotorcycleType.NAKED, owner
                )
        );
        tripId = persistTrip(motorcycle);
        otherTripId = persistTrip(motorcycle);
    }

    @Test
    void shouldPersistAndFindExpensesOrderedByCreation() {
        adapter.save(expenseOf(tripId, "10.2", "64.90", BASE.plusSeconds(60)));
        adapter.save(expenseOf(tripId, "5", "30.00", BASE));
        adapter.save(expenseOf(otherTripId, "8", "50.00", BASE));
        flushAndClear();

        List<TripExpense> expenses = adapter.findByTripId(tripId);

        assertAll(
                () -> assertEquals(2, expenses.size()),
                () -> assertEquals(0, new BigDecimal("5").compareTo(expenses.get(0).getAmount())),
                () -> assertEquals(0, new BigDecimal("10.2").compareTo(expenses.get(1).getAmount())),
                () -> assertEquals(tripId, expenses.get(0).getTripId()),
                () -> assertNotNull(expenses.get(0).getId())
        );
    }

    @Test
    void shouldFindForUpdateOnlyWithinTheSameTrip() {
        TripExpense saved = adapter.save(expenseOf(tripId, "10.2", "64.90", BASE));
        flushAndClear();

        assertAll(
                () -> assertTrue(
                        adapter.findByIdAndTripIdForUpdate(saved.getId(), tripId).isPresent()
                ),
                () -> assertTrue(
                        adapter.findByIdAndTripIdForUpdate(saved.getId(), otherTripId).isEmpty()
                )
        );
    }

    @Test
    void shouldUpdateExistingTripExpense() {
        TripExpense saved = adapter.save(expenseOf(tripId, "10.2", "631.00", BASE));
        flushAndClear();
        TripExpense loaded = adapter.findByIdAndTripIdForUpdate(saved.getId(), tripId)
                .orElseThrow();

        loaded.update(ExpenseCategory.PARKING, new BigDecimal("63.10"), "Centro");
        adapter.save(loaded);
        flushAndClear();

        TripExpense reloaded = adapter.findByTripId(tripId).getFirst();
        assertAll(
                () -> assertEquals(1, adapter.findByTripId(tripId).size()),
                () -> assertEquals(0, new BigDecimal("63.10").compareTo(reloaded.getAmount())),
                () -> assertEquals(ExpenseCategory.PARKING, reloaded.getCategory()),
                () -> assertEquals("Centro", reloaded.getDescription()),
                () -> assertEquals(BASE, reloaded.getCreatedAt())
        );
    }

    @Test
    void shouldDeleteById() {
        TripExpense saved = adapter.save(expenseOf(tripId, "10.2", "64.90", BASE));
        flushAndClear();

        adapter.deleteById(saved.getId());
        flushAndClear();

        assertTrue(adapter.findByTripId(tripId).isEmpty());
    }

    @Test
    void shouldDeleteAllExpensesOfTripOnly() {
        adapter.save(expenseOf(tripId, "10.2", "64.90", BASE));
        adapter.save(expenseOf(tripId, "5", "30.00", BASE));
        adapter.save(expenseOf(otherTripId, "8", "50.00", BASE));
        flushAndClear();

        adapter.deleteByTripId(tripId);
        flushAndClear();

        assertAll(
                () -> assertTrue(adapter.findByTripId(tripId).isEmpty()),
                () -> assertEquals(1, adapter.findByTripId(otherTripId).size())
        );
    }

    private Long persistTrip(MotorcycleEntity motorcycle) {
        return springDataTripRepository.saveAndFlush(new TripEntity(
                null, "Campinas", "Santos", 170.0, TripStatus.IN_PROGRESS,
                TerrainType.ASPHALT, LocalDate.of(2026, 9, 14), motorcycle, null, null
        )).getId();
    }

    private TripExpense expenseOf(Long tripId, String amount, String unused, Instant createdAt) {
        return TripExpense.restore(
                null, tripId, ExpenseCategory.TOLL, new BigDecimal(amount), null, createdAt
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
