package cl.pagoflex.empresas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmpresasServiceApplicationTests {
    @Test
    void clasePrincipalTieneConfiguracionSpringBoot() {
        assertNotNull(EmpresasServiceApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
