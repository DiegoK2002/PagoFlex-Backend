package cl.pagoflex.compromisos.service;

import cl.pagoflex.compromisos.client.*;
import cl.pagoflex.compromisos.model.Compromiso;
import cl.pagoflex.compromisos.repository.CompromisoRepository;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


class CompromisoCoberturaTest {
    CompromisoRepository repo;
    EmpresasClient empresas;
    UsuariosClient usuarios;
    CompromisoService service;
    @BeforeEach void preparar() {
        repo = mock(CompromisoRepository.class);
        empresas = mock(EmpresasClient.class);
        usuarios = mock(UsuariosClient.class);
        service = new CompromisoService(repo, empresas, usuarios);
    }
    Compromiso datos() {
        Compromiso c = new Compromiso();
        c.setIdCompromiso(4L);
        c.setIdUsuario(7L); c.setIdEmpresa(9L);
        c.setNombre(" Cuenta de luz ");
        c.setMonto(new BigDecimal("12000.00"));
        c.setFechaVencimiento(LocalDate.of(2026,11,30));
        c.setEstado("PENDIENTE"); c.setActivo(true);
        return c;
    }
    void referencias() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        when(empresas.buscarPorId(9L)).thenReturn(new EmpresasClient.EmpresaResumen(9L,true));
    }
    static Stream<Compromiso> invalidos() {
        Compromiso a = new Compromiso();
        a.setIdUsuario(null);
        Compromiso b = new Compromiso(); b.setIdUsuario(7L); b.setIdEmpresa(9L);
        Compromiso c = new Compromiso(); c.setIdUsuario(-1L); c.setIdEmpresa(9L);
        c.setNombre("A"); c.setMonto(BigDecimal.ONE); c.setFechaVencimiento(LocalDate.now());
        Compromiso d = new Compromiso(); d.setIdUsuario(7L); d.setIdEmpresa(9L);
        d.setNombre("   "); d.setMonto(BigDecimal.ONE); d.setFechaVencimiento(LocalDate.now());
        Compromiso e = new Compromiso(); e.setIdUsuario(7L); e.setIdEmpresa(9L);
        e.setNombre("x".repeat(101)); e.setMonto(BigDecimal.ONE); e.setFechaVencimiento(LocalDate.now());
        Compromiso f = new Compromiso(); f.setIdUsuario(7L); f.setIdEmpresa(9L);
        f.setNombre("A"); f.setMonto(new BigDecimal("-1")); f.setFechaVencimiento(LocalDate.now());
        Compromiso g = new Compromiso(); g.setIdUsuario(7L); g.setIdEmpresa(9L);
        g.setNombre("A"); g.setMonto(null); g.setFechaVencimiento(LocalDate.now());
        return Stream.of(a,b,c,d,e,f,g);
    }
    @ParameterizedTest @MethodSource("invalidos")
    void rechazaDatosInvalidosSinLlamadasRemotas(Compromiso c) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.registrar(c)).getStatusCode());
        verifyNoInteractions(usuarios, empresas);
    }
    @Test void listaTodos() {
        when(repo.findAll()).thenReturn(List.of(datos()));
        assertEquals(1,service.listarTodos().size());
    }
    @Test void buscaCompromisoExistente() {
        when(repo.findById(4L)).thenReturn(Optional.of(datos()));
        assertEquals(Long.valueOf(4L),service.buscarPorId(4L).getIdCompromiso());
    }
    @Test void buscarInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.buscarPorId(80L)).getStatusCode());
    }
    @Test void actualizaTodosLosCampos() {
        referencias();
        Compromiso previo = datos();
        when(repo.findById(4L)).thenReturn(Optional.of(previo));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Compromiso cambios=datos(); cambios.setNombre(" Nueva deuda ");
        cambios.setMonto(new BigDecimal("999.99"));
        var r = service.actualizar(4L,cambios);
        assertEquals("Nueva deuda",r.getNombre());
        assertEquals(new BigDecimal("999.99"),r.getMonto());
        assertEquals(7L, r.getIdUsuario().longValue());
    }
    @Test void actualizacionInexistenteDevuelve404() {
        referencias();
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            () -> service.actualizar(70L,datos())).getStatusCode());
    }
    @Test void usuarioConIdDiferenteNoSeAcepta() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(8L,true));
        assertStatus(HttpStatus.BAD_GATEWAY);
    }
    @Test void usuarioNuloNoSeAcepta() {
        assertStatus(HttpStatus.BAD_GATEWAY);
    }
    @Test void usuarioInactivoNoSeAcepta() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,false));
        assertStatus(HttpStatus.CONFLICT);
    }
    @Test void usuarioRemotoInexistenteDa404() {
        when(usuarios.buscarPorId(7L)).thenThrow(mock(FeignException.NotFound.class));
        assertStatus(HttpStatus.NOT_FOUND);
    }
    @Test void usuarioRemotoNoDisponibleDa503() {
        when(usuarios.buscarPorId(7L)).thenThrow(mock(FeignException.class));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }
    @Test void empresaNulaNoSeAcepta() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        assertStatus(HttpStatus.BAD_GATEWAY);
    }
    @Test void empresaConIdDiferenteNoSeAcepta() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        when(empresas.buscarPorId(9L)).thenReturn(new EmpresasClient.EmpresaResumen(8L,true));
        assertStatus(HttpStatus.BAD_GATEWAY);
    }
    @Test void empresaInactivaNoSeAcepta() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        when(empresas.buscarPorId(9L)).thenReturn(new EmpresasClient.EmpresaResumen(9L,false));
        assertStatus(HttpStatus.CONFLICT);
    }
    @Test void empresaNoExisteDa404() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        when(empresas.buscarPorId(9L)).thenThrow(mock(FeignException.NotFound.class));
        assertStatus(HttpStatus.NOT_FOUND);
    }
    @Test void empresaRemotaNoDisponibleDa503() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        when(empresas.buscarPorId(9L)).thenThrow(mock(FeignException.class));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }
    @Test void confirmadoRepetidoEsIdempotente() {
        Compromiso c=datos(); c.setEstado("pagado");
        when(repo.findById(4L)).thenReturn(Optional.of(c));
        assertSame(c, service.marcarComoPagado(4L));
    }
    @Test void marcarPagoEnEstadoInvalidoDaConflicto() {
        when(repo.findById(4L)).thenReturn(Optional.of(datos()));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> service.marcarComoPagado(4L)).getStatusCode());
    }
    @Test void marcarPagoInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.marcarComoPagado(4L)).getStatusCode());
    }
    @Test void marcarPagoIdNullDa400() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.marcarComoPagado(null)).getStatusCode());
    }
    @Test void eliminarInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.eliminar(99L)).getStatusCode());
    }
    void assertStatus(HttpStatus esperado) {
        assertEquals(esperado,assertThrows(ResponseStatusException.class,
            () -> service.registrar(datos())).getStatusCode());
        verify(repo, never()).save(any());
    }
}
