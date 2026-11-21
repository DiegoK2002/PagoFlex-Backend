package cl.pagoflex.archivos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@OpenAPIDefinition(info = @Info(title = "PagoFlex - Servicio de Archivos", version = "1.0", description = "Carga, descarga y consulta de archivos y comprobantes."))
@SpringBootApplication
@EnableFeignClients
public class ArchivosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchivosServiceApplication.class, args);
    }
}
