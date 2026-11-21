package cl.pagoflex.usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import cl.pagoflex.usuarios.model.Usuario;
import cl.pagoflex.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Identidad autenticada", description = "Identificación del usuario que presenta credenciales HTTP Basic.")
@RestController
@RequestMapping("/api/v1/usuarios/seguridad")
public class IdentidadController {

    private final UsuarioRepository usuarioRepository;

    public IdentidadController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Operation(summary = "Consultar identidad autenticada",
            description = "Devuelve el identificador y rol de la cuenta autenticada mediante HTTP Basic.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Identidad obtenida"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida o usuario inexistente")
    })
    @GetMapping("/identidad")
    @Transactional(readOnly = true)
    public Identidad obtenerIdentidad(Authentication authentication) {

        Usuario usuario = usuarioRepository
                .findByCorreoIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado"
                ));

        return new Identidad(
                usuario.getIdUsuario(),
                usuario.getRol().getNombreRol()
        );
    }

    public record Identidad(
            Long idUsuario,
            String rol
    ) {
    }
}