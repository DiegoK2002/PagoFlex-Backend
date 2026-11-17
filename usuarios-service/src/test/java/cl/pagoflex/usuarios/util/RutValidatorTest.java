package cl.pagoflex.usuarios.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RutValidatorTest {
    @Test
    void normalizaRutValido() {
        assertEquals("11111111-1", RutValidator.normalizarYValidar(" 11.111.111-1 "));
    }

    @Test
    void rechazaDigitoVerificadorInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> RutValidator.normalizarYValidar("11.111.111-2"));
    }

    @Test
    void rechazaRutVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> RutValidator.normalizarYValidar(" "));
    }

    @Test
    void rechazaFormatoIncorrecto() {
        assertThrows(IllegalArgumentException.class,
                () -> RutValidator.normalizarYValidar("ABCDEFGHI"));
    }
}
