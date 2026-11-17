package cl.pagoflex.pagos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PagosServiceApplicationTests {
    @Test
    void clasePrincipalTieneConfiguracionSpringBoot() {
        assertNotNull(PagosServiceApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
