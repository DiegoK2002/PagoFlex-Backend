package cl.pagoflex.pagos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "usuarios-service")
public interface UsuariosClient {

    @GetMapping("/api/v1/usuarios/{id}")
    UsuarioResumen buscarPorId(@PathVariable("id") Long id);

    @GetMapping("/api/v1/usuarios/seguridad/identidad")
    IdentidadAutenticada obtenerIdentidad(
            @RequestHeader("Authorization") String autorizacion
    );

    record IdentidadAutenticada(
            Long idUsuario,
            String rol
    ) {
    }
    record UsuarioResumen(
            Long idUsuario,
            Boolean activo
    ) {
    }
}
