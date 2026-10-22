package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.repository.FuelEntryRepository;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final FuelEntryRepository fuelEntryRepository;
    private final Clock clock;

    public TripService(
            TripRepository tripRepository,
            FuelEntryRepository fuelEntryRepository,
            Clock clock
    ) {
        this.tripRepository = tripRepository;
        this.fuelEntryRepository = fuelEntryRepository;
        this.clock = clock;
    }

    public Trip scheduleTrip(
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrainType,
            LocalDate tripDate,
            Motorcycle motorcycle
    ) {
        Trip trip = Trip.schedule(
                origin,
                destination,
                distanceKm,
                terrainType,
                tripDate,
                motorcycle,
                clock
        );

        return tripRepository.save(trip);
    }

    public Trip registerCompletedTrip(
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrainType,
            LocalDate tripDate,
            Motorcycle motorcycle
    ) {
        Trip trip = Trip.registerCompleted(
                origin,
                destination,
                distanceKm,
                terrainType,
                tripDate,
                motorcycle,
                clock
        );

        return tripRepository.save(trip);
    }

    @Transactional
    public void changeTripStatusForOwner(
            long tripId,
            Long ownerId,
            TripStatus newStatus,
            boolean updateTripDateToToday,
            Instant occurredAt
    ) {
        Trip trip = findTripByIdForUpdateForOwner(tripId, ownerId);

        LocalDate currentDate = LocalDate.now(clock);
        Instant now = clock.instant();
        Instant effectiveAt = occurredAt != null ? occurredAt : now;
        switch (newStatus) {
            case IN_PROGRESS -> trip.startTrip(
                    currentDate,
                    updateTripDateToToday,
                    effectiveAt,
                    now
            );
            case COMPLETED -> trip.completeTrip(
                    currentDate,
                    updateTripDateToToday,
                    effectiveAt,
                    now
            );
            case PLANNED -> throw new InvalidTripStatusException(
                    "Não é possível reverter uma viagem para o status PLANNED."
            );
        }
        tripRepository.save(trip);
    }

    public long calculateDaysUntilTripForOwner(long tripId, Long ownerId) {
        Trip trip = findTripByIdForOwner(tripId, ownerId);

        return calculateDaysUntilTrip(trip);
    }

    long calculateDaysUntilTrip(Trip trip) {
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new InvalidTripStatusException(
                    "Só é possível calcular os dias restantes de uma viagem planejada."
            );
        }

        return ChronoUnit.DAYS.between(
                LocalDate.now(clock),
                trip.getTripDate()
        );
    }

    @Transactional
    public void deleteTripByIdForOwner(Long tripId, Long ownerId) {
        findTripByIdForUpdateForOwner(tripId, ownerId);
        fuelEntryRepository.deleteByTripId(tripId);
        tripRepository.deleteById(tripId);
    }

    public Trip findTripByIdForOwner(
            Long tripId,
            Long ownerId
    ) {
        return requireOwnedTrip(
                tripRepository.findById(tripId),
                tripId,
                ownerId
        );
    }

    public List<Trip> findTripsByOwnerId(Long ownerId) {
        return tripRepository.findByOwnerId(ownerId);
    }

    public List<Trip> findTripsByOwnerIdAndStatus(Long ownerId, TripStatus status) {
        return tripRepository.findByOwnerIdAndStatus(ownerId, status);
    }

    public Optional<Trip> findInProgressTripByOwnerId(Long ownerId) {
        return findTripsByOwnerIdAndStatus(
                ownerId,
                TripStatus.IN_PROGRESS
        )
                .stream()
                .findFirst();
    }

    public List<Trip> findUpcomingTripsByOwnerId(Long ownerId) {
        return tripRepository.findUpcomingByOwnerIdFromDate(
                ownerId,
                LocalDate.now(clock)
        );
    }

    public Optional<Trip> findNextUpcomingTripByOwnerId(Long ownerId) {
        return tripRepository.findFirstUpcomingByOwnerIdFromDate(
                ownerId,
                LocalDate.now(clock)
        );
    }

    public List<Trip> findTripsByOwnerIdAndTerrain(Long ownerId, TerrainType terrainType) {
        return tripRepository.findByOwnerIdAndTerrain(
                ownerId,
                terrainType
        );
    }

    public List<Trip> findTripsByOwnerIdAndDate(
            Long ownerId,
            LocalDate tripDate
    ) {
        return tripRepository.findByOwnerIdAndTripDate(ownerId, tripDate);
    }

    public List<Trip> findCompletedTripsByOwnerId(Long ownerId) {
        return tripRepository.findCompletedByOwnerIdOrderByTripDateDesc(ownerId);
    }

    private Trip findTripByIdForUpdateForOwner(
            Long tripId,
            Long ownerId
    ) {
        return requireOwnedTrip(
                tripRepository.findByIdForUpdate(tripId),
                tripId,
                ownerId
        );
    }

    private Trip requireOwnedTrip(
            Optional<Trip> tripResult,
            Long tripId,
            Long ownerId
    ) {
        Trip trip = tripResult
                .orElseThrow(() -> new TripNotFoundException(
                        "Viagem com id %d não encontrada".formatted(tripId)
                ));

        if (!trip.getMotorcycle().getOwner().getId().equals(ownerId)) {
            throw new TripAccessDeniedException(
                    "Você não possui permissão para acessar esta viagem."
            );
        }

        return trip;
    }

}
