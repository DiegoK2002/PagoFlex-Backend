package cl.pagoflex.compromisos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CompromisosServiceApplicationTests {
    @Test
    void clasePrincipalTieneConfiguracionSpringBoot() {
        assertNotNull(CompromisosServiceApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
