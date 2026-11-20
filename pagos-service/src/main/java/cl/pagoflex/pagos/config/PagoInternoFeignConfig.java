package cl.pagoflex.pagos.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PagoInternoFeignConfig {

    @Bean
    public RequestInterceptor autenticacionInternaFeign(
            @Value("${PAGOFLEX_INTERNAL_SECRET:}") String secretoInterno
    ) {
        return solicitud -> {
            String ruta = solicitud.path();

            if ("POST".equalsIgnoreCase(solicitud.method())
                    && ruta != null
                    && ruta.startsWith("/internal/compromisos/")
                    && ruta.endsWith("/marcar-pagado")
                    && secretoInterno.length() >= 32) {

                solicitud.header(
                        "X-PagoFlex-Internal-Secret",
                        secretoInterno
                );
            }
        };
    }
}