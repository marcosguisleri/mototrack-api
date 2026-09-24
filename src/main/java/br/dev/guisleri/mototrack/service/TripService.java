package br.dev.guisleri.mototrack.service;

import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripAccessDeniedException;
import br.dev.guisleri.mototrack.exception.TripNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.model.TerrainType;
import br.dev.guisleri.mototrack.model.Trip;
import br.dev.guisleri.mototrack.model.TripStatus;
import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final Clock clock;

    public TripService(TripRepository tripRepository, Clock clock) {
        this.tripRepository = tripRepository;
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

    public void changeTripStatusForOwner(
            long tripId,
            Long ownerId,
            TripStatus newStatus
    ) {
        Trip trip = findTripByIdForOwner(tripId, ownerId);

        trip.changeStatus(newStatus,
                LocalDate.now(clock));
        tripRepository.save(trip);
    }

    public long calculateDaysUntilTripForOwner(long tripId, Long ownerId) {
        Trip trip = findTripByIdForOwner(tripId, ownerId);

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

    public void deleteTripByIdForOwner(Long tripId, Long ownerId) {
        findTripByIdForOwner(tripId, ownerId);
        tripRepository.deleteById(tripId);
    }

    public Trip findTripByIdForOwner(
            Long tripId,
            Long ownerId
    ) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(
                        "Viagem com id %d não encontrada".formatted(tripId)
                ));

        Motorcycle motorcycle = trip.getMotorcycle();

        User user = motorcycle.getOwner();

        if (!user.getId().equals(ownerId)) {
            throw new TripAccessDeniedException(
                    "Você não possui permissão para acessar esta viagem."
            );
        }

        return trip;

    }

    public List<Trip> findTripsByOwnerId(Long ownerId) {
        return tripRepository.findByOwnerId(ownerId);
    }

    public List<Trip> findTripsByOwnerIdAndStatus(Long ownerId, TripStatus status) {
        return tripRepository.findByOwnerIdAndStatus(ownerId, status);
    }

    public List<Trip> findUpcomingTripsByOwnerId(Long ownerId) {
        return tripRepository.findUpcomingFromByOwnerId(
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

}
