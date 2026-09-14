package br.dev.guisleri.mototrack.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MotorcycleTest {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        Motorcycle first = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
        Motorcycle second = new Motorcycle(1, "Yamaha", "Tenere 700", 2024, 689);

        assertEquals(first, second);
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferent() {
        Motorcycle first = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
        Motorcycle second = new Motorcycle(2, "Yamaha", "Tenere 700", 2024, 689);

        assertNotEquals(first, second);
    }

    @Test
    void shouldHaveSameHashCodeWhenIdsAreEqual() {
        Motorcycle first = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
        Motorcycle second = new Motorcycle(1, "Yamaha", "Tenere 700", 2024, 689);

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenModelsMatchButIdsAreDifferent() {
        Motorcycle first = new Motorcycle(1, "Honda", "NX 500", 2025, 471);
        Motorcycle second = new Motorcycle(2, "Honda", "NX 500", 2025, 471);

        assertNotEquals(first, second);
    }
}
