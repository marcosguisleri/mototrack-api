package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.persistence.entity.TripExpenseEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataTripExpenseRepository extends JpaRepository<TripExpenseEntity, Long> {

    List<TripExpenseEntity> findByTrip_IdOrderByCreatedAtAscIdAsc(Long tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM TripExpenseEntity e WHERE e.id = :expenseId AND e.trip.id = :tripId")
    Optional<TripExpenseEntity> findByIdAndTripIdForUpdate(
            @Param("expenseId") long expenseId,
            @Param("tripId") long tripId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TripExpenseEntity e WHERE e.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);

}
