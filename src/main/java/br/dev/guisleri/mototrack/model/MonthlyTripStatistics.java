package br.dev.guisleri.mototrack.model;

public record MonthlyTripStatistics(
        int year,
        int month,
        long tripCount,
        double totalDistanceKm
) {
}
