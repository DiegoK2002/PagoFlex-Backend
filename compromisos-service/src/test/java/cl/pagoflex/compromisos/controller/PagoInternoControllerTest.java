package cl.pagoflex.compromisos.controller;

import cl.pagoflex.compromisos.service.CompromisoService;
import cl.pagoflex.compromisos.model.Compromiso;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class PagoInternoControllerTest {
    @Test void delegaConfirmacionAlServicio() {
        CompromisoService service=mock(CompromisoService.class);
        Compromiso compromiso=new Compromiso();
        compromiso.setEstado("PAGADO");
        when(service.marcarComoPagado(32L)).thenReturn(compromiso);
        var respuesta=new PagoInternoController(service).marcarComoPagado(32L);
        assertEquals(HttpStatus.OK,respuesta.getStatusCode());
        assertSame(compromiso,respuesta.getBody());
        verify(service).marcarComoPagado(32L);
    }
}
