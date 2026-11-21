package cl.pagoflex.usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Verificación de acceso", description = "Consulta interna de permisos administrativos.")
@RestController
@RequestMapping("/api/v1/usuarios")
public class VerificacionAccesoController {

    @Operation(summary = "Comprobar rol administrador",
            description = "Devuelve 204 si las credenciales corresponden a un administrador.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Administrador autorizado"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Sin privilegios de administrador")
    })
    @GetMapping("/verificar-administrador")
    public ResponseEntity<Void> verificarAdministrador() {
        return ResponseEntity.noContent().build();
    }
}