package cl.pagoflex.archivos.service;

import cl.pagoflex.archivos.client.PagosClient;
import cl.pagoflex.archivos.client.UsuariosClient;
import cl.pagoflex.archivos.model.Archivo;
import cl.pagoflex.archivos.repository.ArchivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArchivoServiceTest {
    @TempDir
    Path carpetaTemporal;

    private ArchivoRepository repositorio;
    private PagosClient pagos;
    private UsuariosClient usuarios;
    private ArchivoService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ArchivoRepository.class);
        pagos = mock(PagosClient.class);
        usuarios = mock(UsuariosClient.class);
        servicio = new ArchivoService(repositorio, pagos, usuarios, carpetaTemporal.toString());
    }

    private MockMultipartFile imagen() {
        return new MockMultipartFile("archivo", "avatar.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1});
    }

    @Test
    void rechazaIdUsuarioInvalido() {
        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.subir(0L, null, "AVATAR", imagen()));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(usuarios, pagos);
    }

    @Test
    void rechazaArchivoVacio() {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "x.png", "image/png", new byte[0]);
        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.subir(3L, null, "AVATAR", vacio));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaPdfComoAvatar() {
        var archivo = new MockMultipartFile("archivo", "avatar.pdf", "application/pdf",
                new byte[]{'%', 'P', 'D', 'F', '-'});
        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.subir(3L, null, "AVATAR", archivo));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(usuarios, pagos);
    }

    @Test
    void subeImagenValidaConNombreNormalizado() {
        when(usuarios.buscarPorId(3L)).thenReturn(new UsuariosClient.UsuarioResumen(3L, true));
        when(repositorio.save(any(Archivo.class))).thenAnswer(inv -> inv.getArgument(0));
        Archivo guardado = servicio.subir(3L, null, " avatar ", imagen());
        assertEquals("AVATAR", guardado.getTipo());
        assertEquals("avatar.png", guardado.getNombreOriginal());
        assertEquals(Long.valueOf(3L), guardado.getIdUsuario());
        assertTrue(guardado.getActivo());
        assertTrue((carpetaTemporal.resolve(guardado.getNombreAlmacenado())).toFile().exists());
    }

    @Test
    void listaArchivosPorUsuario() {
        when(repositorio.findByIdUsuario(3L)).thenReturn(List.of(new Archivo()));
        assertEquals(1, servicio.listarPorUsuario(3L).size());
    }

    @Test
    void noDescargaArchivoInactivo() {
        Archivo archivo = new Archivo();
        archivo.setActivo(false);
        when(repositorio.findById(7L)).thenReturn(Optional.of(archivo));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.descargar(7L));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void desactivaArchivoSinBorrarlo() {
        Archivo archivo = new Archivo();
        archivo.setActivo(true);
        when(repositorio.findById(7L)).thenReturn(Optional.of(archivo));
        servicio.eliminar(7L);
        assertFalse(archivo.getActivo());
        verify(repositorio).save(archivo);
    }
}
