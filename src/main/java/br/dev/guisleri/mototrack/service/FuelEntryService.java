package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.FuelEntryNotFoundException;
import br.dev.guisleri.mototrack.model.FuelEntry;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.repository.FuelEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

@Service
public class FuelEntryService {

    private final FuelEntryRepository fuelEntryRepository;
    private final TripService tripService;
    private final Clock clock;

    public FuelEntryService(
            FuelEntryRepository fuelEntryRepository,
            TripService tripService,
            Clock clock
    ) {
        this.fuelEntryRepository = fuelEntryRepository;
        this.tripService = tripService;
        this.clock = clock;
    }

    public FuelEntry registerFuelEntryForOwner(
            long tripId,
            Long ownerId,
            BigDecimal liters,
            BigDecimal totalCost
    ) {
        Trip trip = tripService.findTripByIdForOwner(tripId, ownerId);
        trip.assertAcceptsRecords();

        return fuelEntryRepository.save(
                FuelEntry.register(tripId, liters, totalCost, clock)
        );
    }

    public List<FuelEntry> findFuelEntriesByTripIdForOwner(
            long tripId,
            Long ownerId
    ) {
        tripService.findTripByIdForOwner(tripId, ownerId);

        return fuelEntryRepository.findByTripId(tripId);
    }

    @Transactional
    public FuelEntry updateFuelEntryForOwner(
            long tripId,
            long fuelEntryId,
            Long ownerId,
            BigDecimal liters,
            BigDecimal totalCost
    ) {
        tripService.findTripByIdForOwner(tripId, ownerId);
        FuelEntry fuelEntry = findFuelEntryForUpdate(fuelEntryId, tripId);

        fuelEntry.update(liters, totalCost);

        return fuelEntryRepository.save(fuelEntry);
    }

    @Transactional
    public void deleteFuelEntryForOwner(
            long tripId,
            long fuelEntryId,
            Long ownerId
    ) {
        tripService.findTripByIdForOwner(tripId, ownerId);
        findFuelEntryForUpdate(fuelEntryId, tripId);

        fuelEntryRepository.deleteById(fuelEntryId);
    }

    private FuelEntry findFuelEntryForUpdate(long fuelEntryId, long tripId) {
        return fuelEntryRepository.findByIdAndTripIdForUpdate(fuelEntryId, tripId)
                .orElseThrow(() -> new FuelEntryNotFoundException(
                        "Abastecimento com id %d não encontrado".formatted(fuelEntryId)
                ));
    }

}
