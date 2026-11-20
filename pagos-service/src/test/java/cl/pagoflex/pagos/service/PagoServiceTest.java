package cl.pagoflex.pagos.service;

import cl.pagoflex.pagos.client.CompromisosClient;
import cl.pagoflex.pagos.client.UsuariosClient;
import cl.pagoflex.pagos.model.Pago;
import cl.pagoflex.pagos.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagoServiceTest {
    private PagoRepository repositorio;
    private CompromisosClient compromisos;
    private UsuariosClient usuarios;
    private PagoService servicio;
    private AutorizacionPagoService autorizacion;
    private static final String AUTORIZACION_PRUEBA = "Basic dGVzdDp0ZXN0";

    @BeforeEach
    void preparar() {
        repositorio = mock(PagoRepository.class);
        compromisos = mock(CompromisosClient.class);
        usuarios = mock(UsuariosClient.class);
        autorizacion = mock(AutorizacionPagoService.class);
        servicio = new PagoService(repositorio, compromisos, usuarios, autorizacion);
    }

    private Pago pago() {
        Pago p = new Pago();
        p.setIdPago(4L);
        p.setIdUsuario(3L);
        p.setIdCompromiso(6L);
        p.setMonto(new BigDecimal("18000"));
        p.setFechaPago(LocalDate.of(2026, 11, 16));
        p.setMetodoPago("TRANSFERENCIA");
        p.setActivo(true);
        p.setEstadoSincronizacion("PENDIENTE");
        return p;
    }

    private void prepararReferencias() {
        when(usuarios.buscarPorId(3L)).thenReturn(new UsuariosClient.UsuarioResumen(3L, true));
        when(compromisos.buscarPorIdAutenticado(6L, AUTORIZACION_PRUEBA)).thenReturn(new CompromisosClient.CompromisoResumen(
                6L, 3L, new BigDecimal("18000"), "PENDIENTE", true));
    }

    @Test
    void registraPagoYConfirmaCompromiso() {
        prepararReferencias();
        when(compromisos.marcarComoPagado(6L)).thenReturn(new CompromisosClient.CompromisoResumen(
                6L, 3L, new BigDecimal("18000"), "PAGADO", true));
        when(repositorio.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        Pago resultado = servicio.registrar(pago(), AUTORIZACION_PRUEBA);
        assertEquals("CONFIRMADO", resultado.getEstadoSincronizacion());
        verify(repositorio, times(2)).save(any(Pago.class));
        verify(compromisos).marcarComoPagado(6L);
    }

    @Test
    void rechazaPagoConMontoIncorrecto() {
        prepararReferencias();
        Pago datos = pago();
        datos.setMonto(new BigDecimal("15000"));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(datos, AUTORIZACION_PRUEBA));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaPagoDeOtroUsuario() {
        prepararReferencias();
        Pago datos = pago();
        datos.setIdUsuario(2L);
        when(usuarios.buscarPorId(2L)).thenReturn(new UsuariosClient.UsuarioResumen(2L, true));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(datos, AUTORIZACION_PRUEBA));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaCompromisoYaPagado() {
        prepararReferencias();
        when(compromisos.buscarPorIdAutenticado(6L, AUTORIZACION_PRUEBA)).thenReturn(new CompromisosClient.CompromisoResumen(
                6L, 3L, new BigDecimal("18000"), "PAGADO", true));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(pago(), AUTORIZACION_PRUEBA));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void rechazaPagoActivoDuplicado() {
        prepararReferencias();
        when(repositorio.existsByIdCompromisoAndActivoTrue(6L)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(pago(), AUTORIZACION_PRUEBA));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void confirmaPagoPendienteSinCrearOtro() {
        Pago p = pago();
        when(repositorio.findById(4L)).thenReturn(Optional.of(p));
        when(compromisos.marcarComoPagado(6L)).thenReturn(new CompromisosClient.CompromisoResumen(
                6L, 3L, new BigDecimal("18000"), "PAGADO", true));
        when(repositorio.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("CONFIRMADO", servicio.confirmarPendiente(4L).getEstadoSincronizacion());
        verify(repositorio, times(1)).save(p);
    }

    @Test
    void confirmarPagoYaConfirmadoNoHaceCambios() {
        Pago p = pago();
        p.setEstadoSincronizacion("CONFIRMADO");
        when(repositorio.findById(4L)).thenReturn(Optional.of(p));
        assertSame(p, servicio.confirmarPendiente(4L));
        verify(compromisos, never()).marcarComoPagado(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaPagoConMontoNegativo() {
        Pago p = pago();
        p.setMonto(new BigDecimal("-1"));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(p, AUTORIZACION_PRUEBA));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(usuarios, compromisos);
    }

    @Test
    void desactivaPagoSinBorrarlo() {
        Pago p = pago();
        when(repositorio.findById(4L)).thenReturn(Optional.of(p));
        servicio.eliminar(4L);
        assertFalse(p.getActivo());
        verify(repositorio).save(p);
    }
}
