package cl.pagoflex.usuarios.controller;

import cl.pagoflex.usuarios.model.Usuario;
import cl.pagoflex.usuarios.model.Rol;
import cl.pagoflex.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ControladoresSeguridadTest {
    @Test void estadoIndicaServicioOperativo() {
        assertEquals("operativo",new EstadoController().obtenerEstado().get("estado"));
    }
    @Test void endpointAdministrativoResponde204() {
        assertEquals(HttpStatus.NO_CONTENT,new VerificacionAccesoController().verificarAdministrador().getStatusCode());
    }
    @Test void identidadSeObtienePorCorreoAutenticado() {
        UsuarioRepository repo=mock(UsuarioRepository.class);
        Authentication auth=mock(Authentication.class);
        when(auth.getName()).thenReturn("ana@example.com");
        Usuario usuario=new Usuario(); usuario.setIdUsuario(7L);
        usuario.setRol(new Rol(1L,"USUARIO_FINAL"));
        when(repo.findByCorreoIgnoreCase("ana@example.com")).thenReturn(Optional.of(usuario));
        var identidad=new IdentidadController(repo).obtenerIdentidad(auth);
        assertEquals(Long.valueOf(7L),identidad.idUsuario());
        assertEquals("USUARIO_FINAL",identidad.rol());
        verify(repo).findByCorreoIgnoreCase("ana@example.com");
    }
    @Test void identidadInexistenteDevuelve401() {
        UsuarioRepository repo=mock(UsuarioRepository.class);
        Authentication auth=mock(Authentication.class);
        when(auth.getName()).thenReturn("no@example.com");
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
            () -> new IdentidadController(repo).obtenerIdentidad(auth)).getStatusCode());
    }
}
