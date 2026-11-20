package cl.pagoflex.usuarios.controller;

import cl.pagoflex.usuarios.model.Usuario;
import cl.pagoflex.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/usuarios/seguridad")
public class IdentidadController {

    private final UsuarioRepository usuarioRepository;

    public IdentidadController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

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