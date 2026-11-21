package cl.pagoflex.compromisos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@OpenAPIDefinition(info = @Info(title = "PagoFlex - Servicio de Compromisos", version = "1.0", description = "Asignación, consulta y administración de compromisos."))
@SpringBootApplication
@EnableFeignClients
public class CompromisosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompromisosServiceApplication.class, args);
    }
}
