package cl.pagoflex.reportes.service;

import cl.pagoflex.reportes.client.PagosClient;
import cl.pagoflex.reportes.client.UsuariosClient;
import cl.pagoflex.reportes.model.Reporte;
import cl.pagoflex.reportes.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReporteServiceTest {
    private ReporteRepository repositorio;
    private PagosClient pagos;
    private UsuariosClient usuarios;
    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ReporteRepository.class);
        pagos = mock(PagosClient.class);
        usuarios = mock(UsuariosClient.class);
        servicio = new ReporteService(repositorio, pagos, usuarios);
    }

    private Reporte reporte() {
        Reporte r = new Reporte();
        r.setIdReporte(8L);
        r.setIdUsuario(3L);
        r.setTipo(" RECLAMO ");
        r.setAsunto(" Pago no reflejado ");
        r.setDescripcion(" Solicito revisión ");
        r.setActivo(true);
        return r;
    }

    @Test
    void registraReportePendiente() {
        when(usuarios.buscarPorId(3L)).thenReturn(new UsuariosClient.UsuarioResumen(3L, true));
        when(repositorio.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));
        Reporte registrado = servicio.registrar(reporte());
        assertEquals("PENDIENTE", registrado.getEstado());
        assertEquals("RECLAMO", registrado.getTipo());
        assertTrue(registrado.getActivo());
        verifyNoInteractions(pagos);
    }

    @Test
    void rechazaReporteSinDescripcion() {
        Reporte datos = reporte();
        datos.setDescripcion(" ");
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(datos));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaPagoAsociadoDeOtroUsuario() {
        Reporte datos = reporte();
        datos.setIdPago(4L);
        when(usuarios.buscarPorId(3L)).thenReturn(new UsuariosClient.UsuarioResumen(3L, true));
        when(pagos.buscarPorId(4L)).thenReturn(new PagosClient.PagoResumen(4L, 2L));
        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(datos));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void listaReportesPorAgente() {
        when(repositorio.findByIdAgente(10L)).thenReturn(List.of(reporte()));
        assertEquals(1, servicio.listarPorAgente(10L).size());
    }

    @Test
    void normalizaEstadoDeBusqueda() {
        when(repositorio.findByEstado("EN_PROCESO")).thenReturn(List.of(reporte()));
        assertEquals(1, servicio.listarPorEstado(" en_proceso ").size());
        verify(repositorio).findByEstado("EN_PROCESO");
    }

    @Test
    void rechazaEstadoDeSeguimientoInvalido() {
        when(repositorio.findById(8L)).thenReturn(Optional.of(reporte()));
        Reporte cambios = new Reporte();
        cambios.setEstado("OTRO");
        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.actualizarSeguimiento(8L, cambios));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void actualizaSeguimientoConAgenteYRespuesta() {
        when(repositorio.findById(8L)).thenReturn(Optional.of(reporte()));
        when(repositorio.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));
        Reporte cambios = new Reporte();
        cambios.setEstado(" resuelto ");
        cambios.setRespuesta(" Solucionado ");
        cambios.setIdAgente(10L);
        Reporte resultado = servicio.actualizarSeguimiento(8L, cambios);
        assertEquals("RESUELTO", resultado.getEstado());
        assertEquals("Solucionado", resultado.getRespuesta());
        assertEquals(Long.valueOf(10L), resultado.getIdAgente());
    }

    @Test
    void eliminaReporteLogicamente() {
        Reporte r = reporte();
        when(repositorio.findById(8L)).thenReturn(Optional.of(r));
        servicio.eliminar(8L);
        assertFalse(r.getActivo());
        verify(repositorio).save(r);
    }
}
