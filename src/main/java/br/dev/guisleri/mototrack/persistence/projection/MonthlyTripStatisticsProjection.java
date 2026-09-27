package br.dev.guisleri.mototrack.persistence.projection;

public interface MonthlyTripStatisticsProjection {

    Integer getYear();

    Integer getMonth();

    Long getTripCount();

    Double getTotalDistanceKm();

}
