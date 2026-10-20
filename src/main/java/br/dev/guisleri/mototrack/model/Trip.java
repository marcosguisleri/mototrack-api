package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripDateUpdateRequiredException;

import java.time.Clock;
import java.time.LocalDate;

public class Trip {

    private final Long id;
    private final String origin;
    private final String destination;
    private final double distanceKm;
    private TripStatus status;
    private final TerrainType terrain;
    private LocalDate tripDate;
    private final Motorcycle motorcycle;

    private Trip(
            Long id,
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            TripStatus status
    ) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.terrain = terrain;
        this.tripDate = tripDate;
        this.motorcycle = motorcycle;
        this.status = status;
    }

    public static Trip schedule(
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            Clock clock
    ) {
        LocalDate currentDate = LocalDate.now(clock);

        if (tripDate.isBefore(currentDate)) {
            throw new InvalidTripDateException(
                    "Não é possível planejar uma viagem para uma data passada."
            );
        }

        return new Trip(
                null,
                origin,
                destination,
                distanceKm,
                terrain,
                tripDate,
                motorcycle,
                TripStatus.PLANNED
        );
    }

    public static Trip registerCompleted(
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            Clock clock
    ) {
        LocalDate currentDate = LocalDate.now(clock);

        if (tripDate.isAfter(currentDate)) {
            throw new InvalidTripDateException(
                    "Uma viagem concluída não pode ter data futura."
            );
        }

        return new Trip(
                null,
                origin,
                destination,
                distanceKm,
                terrain,
                tripDate,
                motorcycle,
                TripStatus.COMPLETED
        );
    }

    public static Trip restore(
            Long id,
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            TripStatus status
    ) {
        return new Trip(
                id,
                origin,
                destination,
                distanceKm,
                terrain,
                tripDate,
                motorcycle,
                status
        );
    }

    public void startTrip(
            LocalDate currentDate,
            boolean updateTripDateToToday
    ) {
        if (!status.canTransitionTo(TripStatus.IN_PROGRESS)) {
            throw new InvalidTripStatusException(
                    "Não é possível iniciar uma viagem com status " + status + "."
            );
        }

        if (!tripDate.equals(currentDate)) {
            if (!updateTripDateToToday) {
                throw new TripDateUpdateRequiredException(
                        "A data da viagem é diferente da data atual. " +
                                "Confirme a atualização da data para iniciar a viagem."
                );
            }

            tripDate = currentDate;
        }

        status = TripStatus.IN_PROGRESS;
    }

    public void completeTrip(LocalDate currentDate) {
        if (!status.canTransitionTo(TripStatus.COMPLETED)) {
            throw new InvalidTripStatusException(
                    "Não é possível concluir uma viagem com status " + status + "."
            );
        }

        if (tripDate.isAfter(currentDate)) {
            throw new InvalidTripDateException(
                    "Uma viagem futura não pode ser concluída."
            );
        }

        status = TripStatus.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public TripStatus getStatus() {
        return status;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public Motorcycle getMotorcycle() {
        return motorcycle;
    }

}
