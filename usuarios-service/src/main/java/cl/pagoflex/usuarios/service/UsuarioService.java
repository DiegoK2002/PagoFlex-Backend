package cl.pagoflex.usuarios.service;

import cl.pagoflex.usuarios.dto.LoginRequest;
import cl.pagoflex.usuarios.dto.RegistroUsuarioRequest;
import cl.pagoflex.usuarios.dto.UsuarioResponse;
import cl.pagoflex.usuarios.model.Rol;
import cl.pagoflex.usuarios.model.Usuario;
import cl.pagoflex.usuarios.repository.RolRepository;
import cl.pagoflex.usuarios.repository.UsuarioRepository;
import cl.pagoflex.usuarios.util.RutValidator;
import cl.pagoflex.usuarios.exception.UsuarioDuplicadoException;
import cl.pagoflex.usuarios.dto.ActualizarUsuarioRequest;
import cl.pagoflex.usuarios.dto.CambiarContrasenaRequest;
import cl.pagoflex.usuarios.dto.RecuperarContrasenaRequest;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(
            RegistroUsuarioRequest request
    ) {

        String rut = RutValidator.normalizarYValidar(
                request.rut()
        );

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByRut(rut)) {
            throw new UsuarioDuplicadoException(
                    "El RUT ya está registrado"
            );
        }

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new UsuarioDuplicadoException(
                    "El correo ya está registrado"
            );
        }

        String contrasena = request.contrasena();

        if (contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "La contraseña supera el límite permitido"
            );
        }

        Rol rol = rolRepository
                .findByNombreRol("USUARIO_FINAL")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "El rol de usuario no existe"
                        )
                );

        Usuario usuario = new Usuario();

        usuario.setRut(rut);
        usuario.setNombres(request.nombres().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setCorreo(correo);
        usuario.setComuna(request.comuna().trim());

        usuario.setContrasenaHash(
                passwordEncoder.encode(contrasena)
        );

        usuario.setRol(rol);
        usuario.setActivo(true);

        Usuario guardado = usuarioRepository.save(usuario);

        return convertirRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse iniciarSesion(
            LoginRequest request
    ) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository
                .findByCorreoIgnoreCase(correo)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Correo o contraseña incorrectos"
                        )
                );

        boolean contrasenaCorrecta = passwordEncoder.matches(
                request.contrasena(),
                usuario.getContrasenaHash()
        );

        if (!contrasenaCorrecta) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contraseña incorrectos"
            );
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta está desactivada"
            );
        }

        return convertirRespuesta(usuario);

        
    }


        @Transactional(readOnly = true)
        public UsuarioResponse buscarPorId(Long id) {

        Usuario usuario = usuarioRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        )
                );

        return convertirRespuesta(usuario);
        }


        @Transactional(readOnly = true)
        public java.util.List<UsuarioResponse> listarTodos() {

        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
        }


@Transactional
public UsuarioResponse actualizarUsuario(
        Long id,
        ActualizarUsuarioRequest request
) {

    Usuario usuario = usuarioRepository
            .findById(id)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Usuario no encontrado"
                    )
            );

    String correo = request.correo()
            .trim()
            .toLowerCase(Locale.ROOT);

    usuarioRepository.findByCorreoIgnoreCase(correo)
            .ifPresent(otroUsuario -> {
                if (!otroUsuario.getIdUsuario()
                        .equals(usuario.getIdUsuario())) {
                    throw new UsuarioDuplicadoException(
                            "El correo ya está registrado"
                    );
                }
            });

    usuario.setNombres(request.nombres().trim());
    usuario.setApellidos(request.apellidos().trim());
    usuario.setCorreo(correo);
    usuario.setComuna(request.comuna().trim());

    Usuario actualizado = usuarioRepository.save(usuario);

    return convertirRespuesta(actualizado);
}


@Transactional
public void eliminarUsuario(Long id) {

    Usuario usuario = usuarioRepository
            .findById(id)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Usuario no encontrado"
                    )
            );

    usuario.setActivo(false);

    usuarioRepository.save(usuario);
}


@Transactional
public void cambiarContrasena(
        Long id,
        CambiarContrasenaRequest request
) {

    Usuario usuario = usuarioRepository
            .findById(id)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Usuario no encontrado"
                    )
            );

    if (!Boolean.TRUE.equals(usuario.getActivo())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta está desactivada"
        );
    }

    boolean correcta = passwordEncoder.matches(
            request.contrasenaActual(),
            usuario.getContrasenaHash()
    );

    if (!correcta) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "La contraseña actual es incorrecta"
        );
    }

    if (request.contrasenaNueva()
            .getBytes(StandardCharsets.UTF_8).length > 72) {

        throw new IllegalArgumentException(
                "La contraseña nueva es demasiado larga"
        );
    }

    String nuevoHash = passwordEncoder.encode(
            request.contrasenaNueva()
    );

    usuario.setContrasenaHash(nuevoHash);
    usuarioRepository.save(usuario);
}


@Transactional
public void recuperarContrasena(
        RecuperarContrasenaRequest request
) {

    String rutIngresado = RutValidator.normalizarYValidar(
            request.rut()
    );

    String correo = request.correo()
            .trim()
            .toLowerCase(Locale.ROOT);

    final String datosNoCoinciden = "Los datos ingresados no coinciden";

    Usuario usuario = usuarioRepository
            .findByCorreoIgnoreCase(correo)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            datosNoCoinciden
                    )
            );

    boolean rutCoincide = rutIngresado.equals(usuario.getRut());
    boolean cuentaActiva = Boolean.TRUE.equals(usuario.getActivo());

    if (!rutCoincide || !cuentaActiva) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                datosNoCoinciden
        );
    }

    String contrasenaNueva = request.contrasenaNueva();

    if (contrasenaNueva.getBytes(StandardCharsets.UTF_8).length > 72) {
        throw new IllegalArgumentException(
                "La contraseña nueva es demasiado larga"
        );
    }

    usuario.setContrasenaHash(
            passwordEncoder.encode(contrasenaNueva)
    );

    usuarioRepository.save(usuario);
}







    private UsuarioResponse convertirRespuesta(
            Usuario usuario
    ) {

        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getRut(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getComuna(),
                usuario.getRol().getNombreRol(),
                usuario.getFotoArchivoId(),
                usuario.getActivo()
        );
    }
}
