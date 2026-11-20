package cl.pagoflex.usuarios.config;

import cl.pagoflex.usuarios.model.Rol;
import cl.pagoflex.usuarios.repository.RolRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


class DatosInicialesConfigTest {
    @Test void creaLosTresRolesSiNoExisten() throws Exception {
        RolRepository repo = mock(RolRepository.class);
        when(repo.save(any(Rol.class))).thenAnswer(i -> i.getArgument(0));
        new DatosInicialesConfig().inicializarRoles(repo).run();
        verify(repo, times(3)).save(any(Rol.class));
        verify(repo).save(argThat(r -> r.getIdRol().equals(3L) && "ADMINISTRADOR".equals(r.getNombreRol())));
    }
    @Test void noDuplicaRolesExistentesPorId() throws Exception {
        RolRepository repo = mock(RolRepository.class);
        when(repo.existsById(any())).thenReturn(true);
        new DatosInicialesConfig().inicializarRoles(repo).run();
        verify(repo, never()).save(any());
    }
    @Test void noDuplicaRolesExistentesPorNombre() throws Exception {
        RolRepository repo = mock(RolRepository.class);
        when(repo.findByNombreRol(any())).thenReturn(Optional.of(new Rol(2L,"EXISTENTE")));
        new DatosInicialesConfig().inicializarRoles(repo).run();
        verify(repo, never()).save(any());
    }
    @Test void devuelveCodificadorBCrypt() {
        var encoder = new PasswordConfig().passwordEncoder();
        assertTrue(encoder.matches("clave123456", encoder.encode("clave123456")));
        assertFalse(encoder.matches("erronea", encoder.encode("clave123456")));
    }
}
