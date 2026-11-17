package cl.pagoflex.empresas.service;

import cl.pagoflex.empresas.model.Empresa;
import cl.pagoflex.empresas.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmpresaServiceTest {
    private EmpresaRepository repositorio;
    private EmpresaService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(EmpresaRepository.class);
        servicio = new EmpresaService(repositorio);
    }

    private Empresa datos(String nombre, String categoria) {
        Empresa e = new Empresa();
        e.setNombre(nombre);
        e.setCategoria(categoria);
        return e;
    }

    @Test
    void registraEmpresaConDatosNormalizados() {
        when(repositorio.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));
        Empresa resultado = servicio.registrar(datos(" Empresa Uno ", " Servicios "));
        assertEquals("Empresa Uno", resultado.getNombre());
        assertEquals("Servicios", resultado.getCategoria());
        assertTrue(resultado.getActivo());
    }

    @Test
    void rechazaEmpresaSinNombre() {
        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(datos(" ", "Servicios")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void listaEmpresasDelRepositorio() {
        when(repositorio.findAll()).thenReturn(List.of(datos("A", "Uno")));
        assertEquals(1, servicio.listarTodas().size());
    }

    @Test
    void devuelve404SiEmpresaNoExiste() {
        when(repositorio.findById(15L)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> servicio.buscarPorId(15L));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void actualizaNombreYCategoria() {
        Empresa existente = datos("Antigua", "Otros");
        when(repositorio.findById(1L)).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));
        Empresa actualizada = servicio.actualizar(1L, datos(" Nueva ", " Salud "));
        assertEquals("Nueva", actualizada.getNombre());
        assertEquals("Salud", actualizada.getCategoria());
    }

    @Test
    void eliminaLogicamenteEmpresa() {
        Empresa empresa = datos("Empresa", "Servicios");
        when(repositorio.findById(1L)).thenReturn(Optional.of(empresa));
        servicio.eliminar(1L);
        assertFalse(empresa.getActivo());
        verify(repositorio).save(empresa);
    }
}
