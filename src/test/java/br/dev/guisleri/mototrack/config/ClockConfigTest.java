package br.dev.guisleri.mototrack.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockConfigTest {

    @Test
    void shouldUseExplicitBrazilianBusinessTimeZone() {
        assertEquals(
                ZoneId.of("America/Sao_Paulo"),
                new ClockConfig().clock().getZone()
        );
    }
}
