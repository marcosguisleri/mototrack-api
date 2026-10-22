package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.persistence.entity.FuelEntryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataFuelEntryRepository extends JpaRepository<FuelEntryEntity, Long> {

    List<FuelEntryEntity> findByTrip_IdOrderByCreatedAtAscIdAsc(Long tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FuelEntryEntity f WHERE f.id = :fuelEntryId AND f.trip.id = :tripId")
    Optional<FuelEntryEntity> findByIdAndTripIdForUpdate(
            @Param("fuelEntryId") long fuelEntryId,
            @Param("tripId") long tripId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM FuelEntryEntity f WHERE f.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);

}
