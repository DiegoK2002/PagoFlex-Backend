package cl.pagoflex.pagos.service;

import cl.pagoflex.pagos.model.Pago;
import cl.pagoflex.pagos.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PagoPendienteSchedulerTest {
    @Test
    void intentaConfirmarCadaPagoPendiente() {
        PagoRepository repositorio = mock(PagoRepository.class);
        PagoService servicio = mock(PagoService.class);
        Pago pendiente = new Pago();
        pendiente.setIdPago(4L);
        when(repositorio.findByEstadoSincronizacionAndActivoTrue("PENDIENTE"))
                .thenReturn(List.of(pendiente));
        new PagoPendienteScheduler(repositorio, servicio).recuperarPagosPendientes();
        verify(servicio).confirmarPendiente(4L);
    }

    @Test
    void falloEnUnPagoNoImpideRevisarElSiguiente() {
        PagoRepository repositorio = mock(PagoRepository.class);
        PagoService servicio = mock(PagoService.class);
        Pago uno = new Pago();
        uno.setIdPago(4L);
        Pago dos = new Pago();
        dos.setIdPago(5L);
        when(repositorio.findByEstadoSincronizacionAndActivoTrue("PENDIENTE"))
                .thenReturn(List.of(uno, dos));
        doThrow(new IllegalStateException("sin conexión")).when(servicio).confirmarPendiente(4L);
        assertDoesNotThrow(() -> new PagoPendienteScheduler(repositorio, servicio)
                .recuperarPagosPendientes());
        verify(servicio).confirmarPendiente(5L);
    }
}
