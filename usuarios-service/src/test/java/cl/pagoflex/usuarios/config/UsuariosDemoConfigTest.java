package cl.pagoflex.usuarios.config;

import cl.pagoflex.usuarios.model.Rol;
import cl.pagoflex.usuarios.model.Usuario;
import cl.pagoflex.usuarios.repository.RolRepository;
import cl.pagoflex.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuariosDemoConfigTest {
    @Test
    void creaTresUsuariosConRolesYCorreosEsperadosEnBaseVacia() throws Exception {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(usuarios.count()).thenReturn(0L);
        when(roles.findByNombreRol("USUARIO_FINAL"))
                .thenReturn(Optional.of(new Rol(1L, "USUARIO_FINAL")));
        when(roles.findByNombreRol("AGENTE"))
                .thenReturn(Optional.of(new Rol(2L, "AGENTE")));
        when(roles.findByNombreRol("ADMINISTRADOR"))
                .thenReturn(Optional.of(new Rol(3L, "ADMINISTRADOR")));
        when(encoder.encode(anyString())).thenReturn("hash-demo");

        new UsuariosDemoConfig().inicializarUsuariosDemo(usuarios, roles, encoder).run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios, times(3)).save(captor.capture());
        List<Usuario> creados = captor.getAllValues();
        assertEquals(List.of("crojas@correo.ejemplo", "alagos@correo.ejemplo", "admin@correo.ejemplo"),
                creados.stream().map(Usuario::getCorreo).toList());
        assertEquals(List.of("USUARIO_FINAL", "AGENTE", "ADMINISTRADOR"),
                creados.stream().map(u -> u.getRol().getNombreRol()).toList());
        assertTrue(creados.stream().allMatch(u -> "hash-demo".equals(u.getContrasenaHash())));
        verify(encoder, times(3)).encode("PagoFlex123");
    }

    @Test
    void noCreaUsuariosSiLaBaseYaTieneDatos() throws Exception {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(usuarios.count()).thenReturn(5L);

        new UsuariosDemoConfig().inicializarUsuariosDemo(usuarios, roles, encoder).run();

        verify(usuarios, never()).save(any());
        verifyNoInteractions(roles, encoder);
    }

    @Test
    void creaRolesSiElInicializadorDeRolesAunNoHaCorrIDo() throws Exception {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(usuarios.count()).thenReturn(0L);
        when(roles.save(any(Rol.class))).thenAnswer(i -> i.getArgument(0));
        when(encoder.encode(anyString())).thenReturn("hash-demo");

        CommandLineRunner inicio = new UsuariosDemoConfig()
                .inicializarUsuariosDemo(usuarios, roles, encoder);
        inicio.run();

        verify(roles, times(3)).save(any(Rol.class));
        verify(usuarios, times(3)).save(any(Usuario.class));
    }

    @Test
    void migraSoloLasTresContrasenasDemoAntiguasAlReiniciar() throws Exception {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(usuarios.count()).thenReturn(3L);
        for (String correo : List.of(
                "crojas@correo.ejemplo",
                "alagos@correo.ejemplo",
                "admin@correo.ejemplo")) {
            Usuario usuario = new Usuario();
            usuario.setCorreo(correo);
            usuario.setContrasenaHash("hash-anterior");
            when(usuarios.findByCorreoIgnoreCase(correo)).thenReturn(Optional.of(usuario));
        }
        when(encoder.matches("DemoPagoFlex2026!", "hash-anterior")).thenReturn(true);
        when(encoder.encode("PagoFlex123")).thenReturn("hash-nuevo");

        new UsuariosDemoConfig().inicializarUsuariosDemo(usuarios, roles, encoder).run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios, times(3)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(
                u -> "hash-nuevo".equals(u.getContrasenaHash())));
        verifyNoInteractions(roles);
    }

    @Test
    void respetaContrasenaCambiadaYNoBorraUsuariosExistentes() throws Exception {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(usuarios.count()).thenReturn(4L);
        Usuario usuario = new Usuario();
        usuario.setCorreo("crojas@correo.ejemplo");
        usuario.setContrasenaHash("hash-personalizado");
        when(usuarios.findByCorreoIgnoreCase("crojas@correo.ejemplo"))
                .thenReturn(Optional.of(usuario));
        when(encoder.matches("DemoPagoFlex2026!", "hash-personalizado"))
                .thenReturn(false);

        new UsuariosDemoConfig().inicializarUsuariosDemo(usuarios, roles, encoder).run();

        verify(usuarios, never()).save(any());
        verify(encoder, never()).encode(anyString());
        assertEquals("hash-personalizado", usuario.getContrasenaHash());
        verifyNoInteractions(roles);
    }

}
