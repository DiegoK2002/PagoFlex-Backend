package cl.pagoflex.empresas.service;

import cl.pagoflex.empresas.model.Empresa;
import cl.pagoflex.empresas.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import java.util.Optional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


class EmpresaCoberturaTest {
    EmpresaRepository repo;
    EmpresaService service;

    @BeforeEach void iniciar() {
        repo = mock(EmpresaRepository.class);
        service = new EmpresaService(repo);
    }
    private Empresa datos(String nombre, String categoria) {
        Empresa e = new Empresa();
        e.setNombre(nombre);
        e.setCategoria(categoria);
        return e;
    }
    static Stream<Arguments> datosInvalidos() {
        return Stream.of(
            Arguments.of(null, null), Arguments.of("", "Industria"),
            Arguments.of(" ", "Industria"), Arguments.of(null, "Industria"),
            Arguments.of("Empresa", null), Arguments.of("Empresa", ""),
            Arguments.of("Empresa", "  "), Arguments.of("x".repeat(101), "Servicio"),
            Arguments.of("Empresa", "x".repeat(101))
        );
    }
    @ParameterizedTest @MethodSource("datosInvalidos")
    void registroRechazaCamposInvalidos(String nombre, String categoria) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.registrar(nombre == null && categoria == null ? null : datos(nombre,categoria)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repo);
    }
    @Test void buscarEmpresaExistente() {
        Empresa e = datos("A", "B");
        when(repo.findById(6L)).thenReturn(Optional.of(e));
        assertSame(e, service.buscarPorId(6L));
    }
    @Test void actualizarEmpresaInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.actualizar(55L, datos("A", "B"))).getStatusCode());
        verify(repo, never()).save(any());
    }
    @Test void actualizarEmpresaInvalidaNoAccedeAlRepositorio() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.actualizar(1L, datos("", "B"))).getStatusCode());
        verifyNoInteractions(repo);
    }
    @Test void eliminarEmpresaInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.eliminar(8L)).getStatusCode());
    }
    @Test void listaVaciaEsValida() {
        when(repo.findAll()).thenReturn(List.of());
        assertTrue(service.listarTodas().isEmpty());
    }
}
