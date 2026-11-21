package cl.pagoflex.empresas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "PagoFlex - Servicio de Empresas", version = "1.0", description = "Catálogo y mantenimiento de empresas."))
@SpringBootApplication
public class EmpresasServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmpresasServiceApplication.class, args);
	}

}
