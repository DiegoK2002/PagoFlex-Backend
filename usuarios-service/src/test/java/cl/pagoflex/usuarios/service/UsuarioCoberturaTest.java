package cl.pagoflex.usuarios.service;

import cl.pagoflex.usuarios.dto.*;
import cl.pagoflex.usuarios.exception.UsuarioDuplicadoException;
import cl.pagoflex.usuarios.model.*;
import cl.pagoflex.usuarios.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


class UsuarioCoberturaTest {
    UsuarioRepository repo;
    RolRepository roles;
    PasswordEncoder encoder;
    UsuarioService service;

    @BeforeEach void preparar() {
        repo = mock(UsuarioRepository.class);
        roles = mock(RolRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new UsuarioService(repo, roles, encoder);
    }
    Usuario usuario() {
        Usuario u = new Usuario();
        u.setIdUsuario(7L);
        u.setRut("11111111-1");
        u.setNombres("Ana");
        u.setApellidos("Perez");
        u.setCorreo("ana@example.com");
        u.setComuna("Santiago");
        u.setRol(new Rol(1L,"USUARIO_FINAL"));
        u.setContrasenaHash("hash");
        u.setActivo(true);
        return u;
    }
    RegistroUsuarioRequest registro(String password) {
        return new RegistroUsuarioRequest("11.111.111-1", " Ana ", " Perez ",
            " Ana@EXAMPLE.com ", password, " Santiago ");
    }
    @Test void registroNoPermiteContrasenaMayorA72Bytes() {
        assertThrows(IllegalArgumentException.class, () -> service.registrar(registro("á".repeat(37))));
        verify(repo, never()).save(any());
    }
    @Test void registroNoPermiteFaltaDeRol() {
        assertThrows(IllegalStateException.class, () -> service.registrar(registro("Clave1234!")));
        verify(repo, never()).save(any());
    }
    @Test void sesionRechazaCorreoInexistente() {
        var ex = assertThrows(ResponseStatusException.class,
            () -> service.iniciarSesion(new LoginRequest("NO@EXAMPLE.COM", "Clave1234!")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(repo).findByCorreoIgnoreCase("no@example.com");
    }
    @Test void sesionActivaDevuelveDatos() {
        when(repo.findByCorreoIgnoreCase("ana@example.com")).thenReturn(Optional.of(usuario()));
        when(encoder.matches("bien","hash")).thenReturn(true);
        var resultado = service.iniciarSesion(new LoginRequest(" ANA@EXAMPLE.COM ", "bien"));
        assertEquals(Long.valueOf(7L), resultado.idUsuario());
        assertEquals("Ana", resultado.nombres());
        assertEquals("USUARIO_FINAL", resultado.rol());
    }
    @Test void buscarUsuarioInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.buscarPorId(100L)).getStatusCode());
    }
    @Test void buscarUsuarioDevuelveRolYEstado() {
        when(repo.findById(7L)).thenReturn(Optional.of(usuario()));
        var r = service.buscarPorId(7L);
        assertEquals("USUARIO_FINAL", r.rol());
        assertTrue(r.activo());
    }
    @Test void listaTransformaTodasLasEntidades() {
        Usuario segundo = usuario();
        segundo.setIdUsuario(8L);
        segundo.setCorreo("otro@correo.cl");
        when(repo.findAll()).thenReturn(List.of(usuario(),segundo));
        var respuestas = service.listarTodos();
        assertEquals(2, respuestas.size());
        assertEquals(Long.valueOf(8L), respuestas.get(1).idUsuario());
    }
    @Test void listaVaciaNoFalla() {
        when(repo.findAll()).thenReturn(List.of());
        assertTrue(service.listarTodos().isEmpty());
    }
    @Test void actualizarDatosNormalizados() {
        Usuario u = usuario();
        when(repo.findById(7L)).thenReturn(Optional.of(u));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var r = service.actualizarUsuario(7L, new ActualizarUsuarioRequest(
            " Nueva "," Persona "," NUEVO@CORREO.CL "," Valparaiso "));
        assertEquals("Nueva",r.nombres());
        assertEquals("Persona",r.apellidos());
        assertEquals("nuevo@correo.cl",r.correo());
        assertEquals("Valparaiso",r.comuna());
    }
    @Test void actualizarAceptaMismoCorreoDelMismoUsuario() {
        Usuario u = usuario();
        when(repo.findById(7L)).thenReturn(Optional.of(u));
        when(repo.findByCorreoIgnoreCase("ana@example.com")).thenReturn(Optional.of(u));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("ana@example.com", service.actualizarUsuario(7L,
            new ActualizarUsuarioRequest("Ana", "Perez", "ANA@EXAMPLE.COM", "Santiago")).correo());
    }
    @Test void actualizarRechazaCorreoDeOtroUsuario() {
        Usuario distinto = usuario();
        distinto.setIdUsuario(8L);
        when(repo.findById(7L)).thenReturn(Optional.of(usuario()));
        when(repo.findByCorreoIgnoreCase("otro@example.com")).thenReturn(Optional.of(distinto));
        assertThrows(UsuarioDuplicadoException.class, () -> service.actualizarUsuario(7L,
            new ActualizarUsuarioRequest("Ana", "Perez", "otro@example.com", "Santiago")));
        verify(repo, never()).save(any());
    }
    @Test void actualizarInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.actualizarUsuario(10L, new ActualizarUsuarioRequest(
                "N", "A", "n@example.com", "S"))).getStatusCode());
    }
    @Test void eliminarInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.eliminarUsuario(10L)).getStatusCode());
    }
    @Test void cambioContrasenaExitoso() {
        Usuario u = usuario();
        when(repo.findById(7L)).thenReturn(Optional.of(u));
        when(encoder.matches("actual","hash")).thenReturn(true);
        when(encoder.encode("nueva1234!")).thenReturn("nuevoHash");
        service.cambiarContrasena(7L,new CambiarContrasenaRequest("actual","nueva1234!"));
        assertEquals("nuevoHash",u.getContrasenaHash());
        verify(repo).save(u);
    }
    @Test void cambioContrasenaUsuarioInexistente() {
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            () -> service.cambiarContrasena(7L,new CambiarContrasenaRequest("actual","nueva1234!"))).getStatusCode());
    }
    @Test void cambioContrasenaUsuarioInactivo() {
        Usuario u = usuario(); u.setActivo(false);
        when(repo.findById(7L)).thenReturn(Optional.of(u));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            () -> service.cambiarContrasena(7L, new CambiarContrasenaRequest("actual", "nueva1234!"))).getStatusCode());
        verifyNoInteractions(encoder);
    }
    @Test void cambioContrasenaIncorrecta() {
        when(repo.findById(7L)).thenReturn(Optional.of(usuario()));
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
            () -> service.cambiarContrasena(7L, new CambiarContrasenaRequest("mala", "nueva1234!"))).getStatusCode());
    }
    @Test void cambioContrasenaLargaNoGuarda() {
        when(repo.findById(7L)).thenReturn(Optional.of(usuario()));
        when(encoder.matches("actual","hash")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.cambiarContrasena(7L,
            new CambiarContrasenaRequest("actual", "a".repeat(73))));
        verify(repo, never()).save(any());
    }
    @Test void recuperarCorreoInexistente() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.recuperarContrasena(new RecuperarContrasenaRequest(
                "ana@example.com", "11111111-1", "nueva1234!"))).getStatusCode());
    }
    @Test void recuperarContrasenaDemasiadoLarga() {
        when(repo.findByCorreoIgnoreCase("ana@example.com")).thenReturn(Optional.of(usuario()));
        assertThrows(IllegalArgumentException.class, () -> service.recuperarContrasena(
            new RecuperarContrasenaRequest("ana@example.com", "11111111-1", "a".repeat(73))));
        verify(repo, never()).save(any());
    }
}
