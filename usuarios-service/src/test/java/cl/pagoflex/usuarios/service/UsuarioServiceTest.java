package cl.pagoflex.usuarios.service;

import cl.pagoflex.usuarios.dto.LoginRequest;
import cl.pagoflex.usuarios.dto.RegistroUsuarioRequest;
import cl.pagoflex.usuarios.dto.RecuperarContrasenaRequest;
import cl.pagoflex.usuarios.exception.UsuarioDuplicadoException;
import cl.pagoflex.usuarios.model.Rol;
import cl.pagoflex.usuarios.model.Usuario;
import cl.pagoflex.usuarios.repository.RolRepository;
import cl.pagoflex.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    private UsuarioRepository repositorio;
    private RolRepository roles;
    private PasswordEncoder encoder;
    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        roles = mock(RolRepository.class);
        encoder = mock(PasswordEncoder.class);
        servicio = new UsuarioService(repositorio, roles, encoder);
    }

    private Usuario usuarioActivo() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(3L);
        usuario.setRut("11111111-1");
        usuario.setNombres("Ana");
        usuario.setApellidos("Perez");
        usuario.setCorreo("ana@example.com");
        usuario.setContrasenaHash("hash");
        usuario.setComuna("Santiago");
        usuario.setRol(new Rol(1L, "USUARIO_FINAL"));
        usuario.setActivo(true);
        return usuario;
    }

    @Test
    void registraUsuarioNormalizandoRutYCorreo() {
        when(roles.findByNombreRol("USUARIO_FINAL"))
                .thenReturn(Optional.of(new Rol(1L, "USUARIO_FINAL")));
        when(encoder.encode("Clave1234!")).thenReturn("hash");
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(3L);
            return u;
        });
        var respuesta = servicio.registrar(new RegistroUsuarioRequest(
                "11.111.111-1", " Ana ", " Perez ", " ANA@EXAMPLE.COM ",
                "Clave1234!", " Santiago "));
        assertEquals("11111111-1", respuesta.rut());
        assertEquals("ana@example.com", respuesta.correo());
        assertEquals("USUARIO_FINAL", respuesta.rol());
        verify(repositorio).existsByRut("11111111-1");
        verify(encoder).encode("Clave1234!");
    }

    @Test
    void rechazaRutDuplicado() {
        when(repositorio.existsByRut("11111111-1")).thenReturn(true);
        assertThrows(UsuarioDuplicadoException.class, () ->
                servicio.registrar(new RegistroUsuarioRequest("11111111-1", "Ana", "Perez",
                        "ana@example.com", "Clave1234!", "Santiago")));
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaCorreoDuplicado() {
        when(repositorio.existsByCorreoIgnoreCase("ana@example.com")).thenReturn(true);
        assertThrows(UsuarioDuplicadoException.class, () ->
                servicio.registrar(new RegistroUsuarioRequest("11111111-1", "Ana", "Perez",
                        "ANA@EXAMPLE.COM", "Clave1234!", "Santiago")));
        verify(repositorio, never()).save(any());
    }

    @Test
    void iniciaSesionConCredencialesCorrectas() {
        when(repositorio.findByCorreoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(usuarioActivo()));
        when(encoder.matches("Clave1234!", "hash")).thenReturn(true);
        var respuesta = servicio.iniciarSesion(new LoginRequest(" ANA@EXAMPLE.COM ", "Clave1234!"));
        assertEquals(Long.valueOf(3L), respuesta.idUsuario());
        assertTrue(respuesta.activo());
    }

    @Test
    void rechazaContrasenaIncorrecta() {
        when(repositorio.findByCorreoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(usuarioActivo()));
        var error = assertThrows(ResponseStatusException.class, () ->
                servicio.iniciarSesion(new LoginRequest("ana@example.com", "incorrecta")));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void rechazaCuentaInactiva() {
        Usuario usuario = usuarioActivo();
        usuario.setActivo(false);
        when(repositorio.findByCorreoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(usuario));
        when(encoder.matches("Clave1234!", "hash")).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () ->
                servicio.iniciarSesion(new LoginRequest("ana@example.com", "Clave1234!")));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void desactivaUsuarioSinBorrarlo() {
        Usuario usuario = usuarioActivo();
        when(repositorio.findById(3L)).thenReturn(Optional.of(usuario));
        servicio.eliminarUsuario(3L);
        assertFalse(usuario.getActivo());
        verify(repositorio).save(usuario);
        verify(repositorio, never()).delete(any());
    }

    @Test
    void recuperaContrasenaActualizandoHash() {
        Usuario usuario = usuarioActivo();
        when(repositorio.findByCorreoIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(usuario));
        when(encoder.encode("NuevaClave123!")).thenReturn("nuevo-hash");
        servicio.recuperarContrasena(new RecuperarContrasenaRequest(
                " ANA@EXAMPLE.COM ", "NuevaClave123!"));
        assertEquals("nuevo-hash", usuario.getContrasenaHash());
        verify(repositorio).save(usuario);
    }
}
