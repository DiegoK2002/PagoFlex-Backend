package cl.pagoflex.pagos.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "compromisos-service")
public interface CompromisosClient {

    @GetMapping("/api/v1/compromisos/{id}")
    CompromisoResumen buscarPorId(
            @PathVariable("id") Long id
    );

    @PostMapping("/internal/compromisos/{id}/marcar-pagado")
    CompromisoResumen marcarComoPagado(
            @PathVariable("id") Long id
    );

    record CompromisoResumen(
            Long idCompromiso,
            Long idUsuario,
            BigDecimal monto,
            String estado,
            Boolean activo
    ) {
    }
}
