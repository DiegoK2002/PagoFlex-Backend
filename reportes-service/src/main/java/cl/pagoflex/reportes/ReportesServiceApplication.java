package cl.pagoflex.reportes;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@OpenAPIDefinition(info = @Info(title = "PagoFlex - Servicio de Reportes", version = "1.0", description = "Creación y seguimiento de incidencias."))
@SpringBootApplication
@EnableFeignClients
public class ReportesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportesServiceApplication.class, args);
    }
}
