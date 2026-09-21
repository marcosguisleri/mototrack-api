package br.dev.guisleri.mototrack.model;

public enum TripStatus {
    PLANNED, IN_PROGRESS, COMPLETED;

    public boolean canTransitionTo(TripStatus newStatus) {
        return switch (this) {
            case PLANNED -> newStatus == IN_PROGRESS;
            case IN_PROGRESS -> newStatus == COMPLETED;
            case COMPLETED -> false;
        };
    }
}
