package cl.pagoflex.usuarios.service;

import cl.pagoflex.usuarios.model.*;
import cl.pagoflex.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class UsuarioDetallesServiceTest {
    @Test void cargaUsuarioConAutoridadCorrecta() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setCorreo("admin@example.com");
        usuario.setContrasenaHash("hash");
        usuario.setRol(new Rol(3L, "ADMINISTRADOR"));
        usuario.setActivo(true);
        when(repo.findByCorreoIgnoreCase("admin@example.com")).thenReturn(Optional.of(usuario));
        var detalles = new UsuarioDetallesService(repo).loadUserByUsername("admin@example.com");
        assertEquals("admin@example.com", detalles.getUsername());
        assertEquals("hash", detalles.getPassword());
        assertTrue(detalles.isEnabled());
        assertTrue(detalles.getAuthorities().stream().anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority())));
    }
    @Test void cuentaDesactivadaNoPuedeAutenticarse() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setCorreo("ana@example.com");
        usuario.setContrasenaHash("hash");
        usuario.setRol(new Rol(1L, "USUARIO_FINAL"));
        usuario.setActivo(false);
        when(repo.findByCorreoIgnoreCase("ana@example.com")).thenReturn(Optional.of(usuario));
        assertFalse(new UsuarioDetallesService(repo).loadUserByUsername("ana@example.com").isEnabled());
    }
    @Test void usuarioInexistenteDaErrorDeAutenticacion() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        assertThrows(UsernameNotFoundException.class,
            () -> new UsuarioDetallesService(repo).loadUserByUsername("sin@correo.cl"));
    }
}
