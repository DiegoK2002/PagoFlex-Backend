
package cl.pagoflex.usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Estado de usuarios", description = "Comprobación básica del estado del servicio.")
@RestController
@RequestMapping("/api/v1/usuarios")
public class EstadoController {

    @Operation(summary = "Consultar estado del servicio",
            description = "Devuelve el nombre del microservicio y su estado operativo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado del servicio")
    })
    @GetMapping("/estado")
    public Map<String, String> obtenerEstado() {
        return Map.of(
            "servicio", "usuarios-service",
            "estado", "operativo"
        );
    }
}
