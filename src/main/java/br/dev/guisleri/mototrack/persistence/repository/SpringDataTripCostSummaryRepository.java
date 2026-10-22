package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.persistence.entity.TripEntity;
import br.dev.guisleri.mototrack.persistence.projection.ExpenseCategoryCostProjection;
import br.dev.guisleri.mototrack.persistence.projection.FuelCostProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataTripCostSummaryRepository
        extends Repository<TripEntity, Long> {

    @Query("""
        SELECT
            COUNT(f) AS entryCount,
            SUM(f.liters) AS liters,
            SUM(f.totalCost) AS totalCost
        FROM FuelEntryEntity f
        WHERE f.trip.id = :tripId
        """)
    FuelCostProjection summarizeFuelByTripId(@Param("tripId") Long tripId);

    @Query("""
        SELECT
            e.category AS category,
            COUNT(e) AS expenseCount,
            SUM(e.amount) AS total
        FROM TripExpenseEntity e
        WHERE e.trip.id = :tripId
        GROUP BY e.category
        """)
    List<ExpenseCategoryCostProjection> summarizeExpensesByCategoryForTripId(
            @Param("tripId") Long tripId
    );

}
