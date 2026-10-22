package br.dev.guisleri.mototrack.repository;

import br.dev.guisleri.mototrack.model.FuelEntry;

import java.util.List;
import java.util.Optional;

public interface FuelEntryRepository {

    FuelEntry save(FuelEntry fuelEntry);

    List<FuelEntry> findByTripId(long tripId);

    Optional<FuelEntry> findByIdAndTripIdForUpdate(long fuelEntryId, long tripId);

    void deleteById(Long fuelEntryId);

    void deleteByTripId(Long tripId);

}
