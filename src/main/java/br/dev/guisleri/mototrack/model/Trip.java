package br.dev.guisleri.mototrack.model;

import br.dev.guisleri.mototrack.exception.InvalidTripDateException;
import br.dev.guisleri.mototrack.exception.InvalidTripStatusException;
import br.dev.guisleri.mototrack.exception.TripDateUpdateRequiredException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public class Trip {

    private final Long id;
    private final String origin;
    private final String destination;
    private final double distanceKm;
    private TripStatus status;
    private final TerrainType terrain;
    private LocalDate tripDate;
    private final Motorcycle motorcycle;
    private Instant startedAt;
    private Instant completedAt;

    private Trip(
            Long id,
            String origin,
            String destination,
            double distanceKm,
            TerrainType terrain,
            LocalDate tripDate,
            Motorcycle motorcycle,
            TripStatus status,
            Instant startedAt,
            Instant completedAt
    ) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.terrain = terrain;
        this.tripDate = tripDate;
        this.motorcycle = motorcycle;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
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
                TripStatus.PLANNED,
                null,
                null
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
                TripStatus.COMPLETED,
                null,
                null
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
            TripStatus status,
            Instant startedAt,
            Instant completedAt
    ) {
        return new Trip(
                id,
                origin,
                destination,
                distanceKm,
                terrain,
                tripDate,
                motorcycle,
                status,
                startedAt,
                completedAt
        );
    }

    public void startTrip(
            LocalDate currentDate,
            boolean updateTripDateToToday,
            Instant startedAt,
            Instant now
    ) {
        if (!status.canTransitionTo(TripStatus.IN_PROGRESS)) {
            throw new InvalidTripStatusException(
                    "Não é possível iniciar uma viagem com status " + status + "."
            );
        }

        if (startedAt.isAfter(now)) {
            throw new InvalidTripDateException(
                    "O horário de início da viagem não pode estar no futuro."
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

        this.startedAt = startedAt;
        status = TripStatus.IN_PROGRESS;
    }

    public void completeTrip(
            LocalDate currentDate,
            boolean updateTripDateToToday,
            Instant completedAt,
            Instant now
    ) {
        if (!status.canTransitionTo(TripStatus.COMPLETED)) {
            throw new InvalidTripStatusException(
                    "Não é possível concluir uma viagem com status " + status + "."
            );
        }

        if (completedAt.isAfter(now)) {
            throw new InvalidTripDateException(
                    "O horário de conclusão da viagem não pode estar no futuro."
            );
        }

        if (startedAt != null && completedAt.isBefore(startedAt)) {
            throw new InvalidTripDateException(
                    "O horário de conclusão não pode ser anterior ao horário de início."
            );
        }

        // A trip in progress has already started, so a future date is an
        // inconsistency to be fixed with the user's confirmation, not a dead end.
        if (tripDate.isAfter(currentDate)) {
            if (!updateTripDateToToday) {
                throw new TripDateUpdateRequiredException(
                        "A data da viagem é posterior à data atual. " +
                                "Confirme a atualização da data para concluir a viagem."
                );
            }

            tripDate = currentDate;
        }

        this.completedAt = completedAt;
        status = TripStatus.COMPLETED;
    }

    public void assertAcceptsRecords() {
        if (status == TripStatus.PLANNED) {
            throw new InvalidTripStatusException(
                    "Só é possível registrar abastecimentos e gastos em viagens " +
                            "iniciadas ou concluídas."
            );
        }
    }

    public Optional<Duration> duration() {
        if (startedAt == null || completedAt == null) {
            return Optional.empty();
        }

        return Optional.of(Duration.between(startedAt, completedAt));
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

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

}
