package br.dev.guisleri.mototrack.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MotorcycleTest {

    private static final User OWNER = User.restore(
            1L,
            "Marcos",
            "marcos@example.com",
            "password-hash"
    );

    // -------------------------------------------------------------------------
    // Registro de motocicletas
    // -------------------------------------------------------------------------

    @Test
    void shouldRegisterMotorcycleWithoutIdAndWithColor() {
        Motorcycle motorcycle = Motorcycle.register(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471,
                MotorcycleType.NAKED,
                OWNER
        );

        assertEquals("Black", motorcycle.getColor());
        assertEquals(OWNER, motorcycle.getOwner());
        assertNull(motorcycle.getId());
    }

    @Test
    void shouldKeepTheTypeWhenRegistering() {
        Motorcycle motorcycle = Motorcycle.register(
                "Honda",
                "CB 500X",
                "Red",
                2024,
                471,
                MotorcycleType.BIG_TRAIL,
                OWNER
        );

        assertEquals(MotorcycleType.BIG_TRAIL, motorcycle.getType());
    }

    @Test
    void shouldAllowMissingTypeWhenRestoringLegacyMotorcycle() {
        Motorcycle motorcycle = Motorcycle.restore(
                1L,
                "Honda",
                "CG 160",
                "Red",
                2020,
                160,
                null,
                OWNER
        );

        assertNull(motorcycle.getType());
    }

    // -------------------------------------------------------------------------
    // Identidade, igualdade e hash code por ID
    // -------------------------------------------------------------------------

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        Motorcycle first = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, OWNER
        );
        Motorcycle second = Motorcycle.restore(
                1L, "Yamaha", "Tenere 700", "Blue", 2024, 689, MotorcycleType.NAKED, OWNER
        );

        assertEquals(first, second);
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferent() {
        Motorcycle first = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, OWNER
        );
        Motorcycle second = Motorcycle.restore(
                2L, "Yamaha", "Tenere 700", "Blue", 2024, 689, MotorcycleType.NAKED, OWNER
        );

        assertNotEquals(first, second);
    }

    @Test
    void shouldHaveSameHashCodeWhenIdsAreEqual() {
        Motorcycle first = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, OWNER
        );
        Motorcycle second = Motorcycle.restore(
                1L, "Yamaha", "Tenere 700", "Blue", 2024, 689, MotorcycleType.NAKED, OWNER
        );

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenModelsMatchButIdsAreDifferent() {
        Motorcycle first = Motorcycle.restore(
                1L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, OWNER
        );
        Motorcycle second = Motorcycle.restore(
                2L, "Honda", "NX 500", "Black", 2025, 471, MotorcycleType.NAKED, OWNER
        );

        assertNotEquals(first, second);
    }
}
