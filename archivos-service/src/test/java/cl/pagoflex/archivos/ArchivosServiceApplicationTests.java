package cl.pagoflex.archivos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArchivosServiceApplicationTests {
    @Test
    void clasePrincipalTieneConfiguracionSpringBoot() {
        assertNotNull(ArchivosServiceApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
