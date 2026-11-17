package cl.pagoflex.reportes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReportesServiceApplicationTests {
    @Test
    void clasePrincipalTieneConfiguracionSpringBoot() {
        assertNotNull(ReportesServiceApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
