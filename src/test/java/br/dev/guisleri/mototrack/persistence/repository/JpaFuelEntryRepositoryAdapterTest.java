package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.FuelEntry;
import br.dev.guisleri.mototrack.model.MotorcycleType;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.persistence.entity.MotorcycleEntity;
import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.FuelEntryMapper;
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
        JpaFuelEntryRepositoryAdapter.class,
        FuelEntryMapper.class
})
class JpaFuelEntryRepositoryAdapterTest {

    private static final Instant BASE = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired
    private JpaFuelEntryRepositoryAdapter adapter;

    @Autowired
    private SpringDataFuelEntryRepository springDataFuelEntryRepository;

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
    void shouldPersistAndFindFuelEntriesOrderedByCreation() {
        adapter.save(entry(tripId, "10.2", "64.90", BASE.plusSeconds(60)));
        adapter.save(entry(tripId, "5", "30.00", BASE));
        adapter.save(entry(otherTripId, "8", "50.00", BASE));
        flushAndClear();

        List<FuelEntry> entries = adapter.findByTripId(tripId);

        assertAll(
                () -> assertEquals(2, entries.size()),
                () -> assertEquals(0, new BigDecimal("5").compareTo(entries.get(0).getLiters())),
                () -> assertEquals(0, new BigDecimal("10.2").compareTo(entries.get(1).getLiters())),
                () -> assertEquals(tripId, entries.get(0).getTripId()),
                () -> assertNotNull(entries.get(0).getId())
        );
    }

    @Test
    void shouldFindForUpdateOnlyWithinTheSameTrip() {
        FuelEntry saved = adapter.save(entry(tripId, "10.2", "64.90", BASE));
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
    void shouldUpdateExistingFuelEntry() {
        FuelEntry saved = adapter.save(entry(tripId, "10.2", "631.00", BASE));
        flushAndClear();
        FuelEntry loaded = adapter.findByIdAndTripIdForUpdate(saved.getId(), tripId)
                .orElseThrow();

        loaded.update(new BigDecimal("10.2"), new BigDecimal("63.10"));
        adapter.save(loaded);
        flushAndClear();

        FuelEntry reloaded = adapter.findByTripId(tripId).getFirst();
        assertAll(
                () -> assertEquals(1, adapter.findByTripId(tripId).size()),
                () -> assertEquals(0, new BigDecimal("63.10").compareTo(reloaded.getTotalCost())),
                () -> assertEquals(BASE, reloaded.getCreatedAt())
        );
    }

    @Test
    void shouldDeleteById() {
        FuelEntry saved = adapter.save(entry(tripId, "10.2", "64.90", BASE));
        flushAndClear();

        adapter.deleteById(saved.getId());
        flushAndClear();

        assertTrue(adapter.findByTripId(tripId).isEmpty());
    }

    @Test
    void shouldDeleteAllEntriesOfTripOnly() {
        adapter.save(entry(tripId, "10.2", "64.90", BASE));
        adapter.save(entry(tripId, "5", "30.00", BASE));
        adapter.save(entry(otherTripId, "8", "50.00", BASE));
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

    private FuelEntry entry(Long tripId, String liters, String cost, Instant createdAt) {
        return FuelEntry.restore(
                null, tripId, new BigDecimal(liters), new BigDecimal(cost), createdAt
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
