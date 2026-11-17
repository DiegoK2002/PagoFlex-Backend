package cl.pagoflex.usuarios;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UsuariosServiceApplicationTests {
    @Test
    void clasePrincipalTieneConfiguracionSpringBoot() {
        assertNotNull(UsuariosServiceApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
