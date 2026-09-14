package br.dev.guisleri.mototrack.model;

public enum TripStatus {
    PLANNED, IN_PROGRESS, COMPLETED;

    public boolean canTransitionTo(TripStatus next) {
        return switch (this) {
            case PLANNED -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == COMPLETED;
            case COMPLETED -> false;
        };
    }
}

