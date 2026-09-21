package br.dev.guisleri.mototrack.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MotorcycleTest {

    @Test
    void shouldRegisterMotorcycleWithoutIdAndWithColor() {
        Motorcycle motorcycle = Motorcycle.register(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        );

        assertEquals("Black", motorcycle.getColor());
        assertNull(motorcycle.getId());
    }

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        Motorcycle first = Motorcycle.restore(1L, "Honda", "NX 500", "Black", 2025, 471);
        Motorcycle second = Motorcycle.restore(1L, "Yamaha", "Tenere 700", "Blue", 2024, 689);

        assertEquals(first, second);
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferent() {
        Motorcycle first = Motorcycle.restore(1L, "Honda", "NX 500", "Black", 2025, 471);
        Motorcycle second = Motorcycle.restore(2L, "Yamaha", "Tenere 700", "Blue", 2024, 689);

        assertNotEquals(first, second);
    }

    @Test
    void shouldHaveSameHashCodeWhenIdsAreEqual() {
        Motorcycle first = Motorcycle.restore(1L, "Honda", "NX 500", "Black", 2025, 471);
        Motorcycle second = Motorcycle.restore(1L, "Yamaha", "Tenere 700", "Blue", 2024, 689);

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenModelsMatchButIdsAreDifferent() {
        Motorcycle first = Motorcycle.restore(1L, "Honda", "NX 500", "Black", 2025, 471);
        Motorcycle second = Motorcycle.restore(2L, "Honda", "NX 500", "Black", 2025, 471);

        assertNotEquals(first, second);
    }
}
