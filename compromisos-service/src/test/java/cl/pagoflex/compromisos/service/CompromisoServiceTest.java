package cl.pagoflex.compromisos.service;

import cl.pagoflex.compromisos.client.EmpresasClient;
import cl.pagoflex.compromisos.client.UsuariosClient;
import cl.pagoflex.compromisos.model.Compromiso;
import cl.pagoflex.compromisos.repository.CompromisoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompromisoServiceTest {
    private CompromisoRepository repositorio;
    private EmpresasClient empresas;
    private UsuariosClient usuarios;
    private CompromisoService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(CompromisoRepository.class);
        empresas = mock(EmpresasClient.class);
        usuarios = mock(UsuariosClient.class);
        servicio = new CompromisoService(repositorio, empresas, usuarios);
    }

    private Compromiso compromiso() {
        Compromiso c = new Compromiso();
        c.setIdCompromiso(6L);
        c.setIdUsuario(3L);
        c.setIdEmpresa(1L);
        c.setNombre(" Cuenta mensual ");
        c.setMonto(new BigDecimal("18000"));
        c.setFechaVencimiento(LocalDate.of(2026, 12, 1));
        c.setEstado("PENDIENTE");
        c.setActivo(true);
        return c;
    }

    private void prepararReferencias() {
        when(usuarios.buscarPorId(3L)).thenReturn(new UsuariosClient.UsuarioResumen(3L, true));
        when(empresas.buscarPorId(1L)).thenReturn(new EmpresasClient.EmpresaResumen(1L, true));
    }

    @Test
    void registraCompromisoPendiente() {
        prepararReferencias();
        when(repositorio.save(any(Compromiso.class))).thenAnswer(inv -> inv.getArgument(0));
        Compromiso nuevo = servicio.registrar(compromiso());
        assertEquals("Cuenta mensual", nuevo.getNombre());
        assertEquals("PENDIENTE", nuevo.getEstado());
        assertTrue(nuevo.getActivo());
        verify(repositorio).save(any(Compromiso.class));
    }

    @Test
    void rechazaMontoCero() {
        Compromiso c = compromiso();
        c.setMonto(BigDecimal.ZERO);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(c));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(usuarios, empresas);
    }

    @Test
    void rechazaEmpresaInactiva() {
        prepararReferencias();
        when(empresas.buscarPorId(1L)).thenReturn(new EmpresasClient.EmpresaResumen(1L, false));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(compromiso()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void consultaCompromisosPorUsuario() {
        when(repositorio.findByIdUsuario(3L)).thenReturn(List.of(compromiso()));
        assertEquals(1, servicio.listarPorUsuario(3L).size());
    }

    @Test
    void cambiaEstadoDePendienteAPagado() {
        Compromiso pagado = compromiso();
        pagado.setEstado("PAGADO");
        when(repositorio.marcarComoPagadoSiPendiente(6L)).thenReturn(1);
        when(repositorio.findById(6L)).thenReturn(Optional.of(pagado));
        assertEquals("PAGADO", servicio.marcarComoPagado(6L).getEstado());
    }

    @Test
    void confirmarPagoRepetidoEsIdempotente() {
        Compromiso pagado = compromiso();
        pagado.setEstado("PAGADO");
        when(repositorio.marcarComoPagadoSiPendiente(6L)).thenReturn(0);
        when(repositorio.findById(6L)).thenReturn(Optional.of(pagado));
        assertEquals("PAGADO", servicio.marcarComoPagado(6L).getEstado());
    }

    @Test
    void rechazaCambioDeEstadoDeCompromisoInactivo() {
        Compromiso inactivo = compromiso();
        inactivo.setActivo(false);
        when(repositorio.findById(6L)).thenReturn(Optional.of(inactivo));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.marcarComoPagado(6L));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void rechazaIdDeCompromisoInvalido() {
        var error = assertThrows(ResponseStatusException.class, () -> servicio.marcarComoPagado(0L));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void desactivaCompromisoSinBorrarlo() {
        Compromiso c = compromiso();
        when(repositorio.findById(6L)).thenReturn(Optional.of(c));
        servicio.eliminar(6L);
        assertFalse(c.getActivo());
        verify(repositorio).save(c);
    }
}
