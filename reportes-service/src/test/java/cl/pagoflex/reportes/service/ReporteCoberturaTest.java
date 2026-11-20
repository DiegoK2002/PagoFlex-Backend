package cl.pagoflex.reportes.service;

import cl.pagoflex.reportes.client.*;
import cl.pagoflex.reportes.model.Reporte;
import cl.pagoflex.reportes.repository.ReporteRepository;
import feign.FeignException;
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


class ReporteCoberturaTest {
    ReporteRepository repo;
    UsuariosClient usuarios;
    PagosClient pagos;
    ReporteService service;
    @BeforeEach void preparar() {
        repo=mock(ReporteRepository.class);
        usuarios=mock(UsuariosClient.class);
        pagos=mock(PagosClient.class);
        service=new ReporteService(repo,pagos,usuarios);
    }
    Reporte datos() {
        Reporte r=new Reporte();
        r.setIdReporte(8L); r.setIdUsuario(7L);
        r.setTipo(" RECLAMO "); r.setAsunto(" Pago ");
        r.setDescripcion(" Una solicitud ");
        r.setEstado("PENDIENTE"); r.setActivo(true);
        return r;
    }
    void usuarioActivo() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
    }
    void referenciasPago() {
        usuarioActivo();
        when(pagos.buscarPorId(23L)).thenReturn(new PagosClient.PagoResumen(23L,7L));
    }
    static Stream<Reporte> invalidos() {
        Reporte a=new Reporte();
        Reporte b=new Reporte(); b.setIdUsuario(0L);
        Reporte c=new Reporte(); c.setIdUsuario(7L); c.setTipo(" ");
        Reporte d=new Reporte(); d.setIdUsuario(7L); d.setTipo("RECLAMO");
        Reporte e=new Reporte(); e.setIdUsuario(7L); e.setTipo("RECLAMO"); e.setAsunto(" ");
        Reporte f=new Reporte(); f.setIdUsuario(7L); f.setTipo("RECLAMO"); f.setAsunto("A"); f.setDescripcion(" ");
        Reporte g=new Reporte(); g.setIdUsuario(7L); g.setTipo("RECLAMO"); g.setAsunto("A"); g.setDescripcion("B"); g.setIdPago(-1L);
        Reporte h=new Reporte(); h.setIdUsuario(7L); h.setTipo("x".repeat(51)); h.setAsunto("A"); h.setDescripcion("B");
        Reporte i=new Reporte(); i.setIdUsuario(7L); i.setTipo("R"); i.setAsunto("x".repeat(151)); i.setDescripcion("B");
        Reporte j=new Reporte(); j.setIdUsuario(7L); j.setTipo("R"); j.setAsunto("A"); j.setDescripcion("x".repeat(2001));
        return Stream.of(a,b,c,d,e,f,g,h,i,j);
    }
    @ParameterizedTest @MethodSource("invalidos")
    void rechazaDatosInvalidos(Reporte datos) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.registrar(datos)).getStatusCode());
        verifyNoInteractions(usuarios,pagos);
    }
    @Test void registroAsociadoAPagoDelUsuario() {
        referenciasPago();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Reporte r=datos(); r.setIdPago(23L);
        var resultado=service.registrar(r);
        assertEquals("PENDIENTE",resultado.getEstado());
        assertEquals(23L,resultado.getIdPago().longValue());
        assertEquals("Pago",resultado.getAsunto());
    }
    @Test void listaTodos() {
        when(repo.findAll()).thenReturn(List.of(datos()));
        assertEquals(1,service.listarTodos().size());
    }
    @Test void listaPorUsuario() {
        when(repo.findByIdUsuario(7L)).thenReturn(List.of(datos()));
        assertEquals(1,service.listarPorUsuario(7L).size());
    }
    @Test void listaPorPago() {
        when(repo.findByIdPago(23L)).thenReturn(List.of(datos()));
        assertEquals(1,service.listarPorPago(23L).size());
    }
    @Test void listaEstadoResuelto() {
        service.listarPorEstado(" resuelto ");
        verify(repo).findByEstado("RESUELTO");
    }
    @Test void listaEstadoPendiente() {
        service.listarPorEstado(" pendiente ");
        verify(repo).findByEstado("PENDIENTE");
    }
    @Test void listarEstadoVacioEsInvalido() {
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> service.listarPorEstado("  ")).getStatusCode());
    }
    @Test void listarEstadoInvalidoEsRechazado() {
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> service.listarPorEstado("OTRO")).getStatusCode());
    }
    @Test void buscarExistente() {
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        assertEquals(8L,service.buscarPorId(8L).getIdReporte().longValue());
    }
    @Test void buscarInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.buscarPorId(999L)).getStatusCode());
    }
    @Test void noPermiteUsuarioInactivoEnRegistro() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,false));
        assertRegistro(HttpStatus.CONFLICT);
    }
    @Test void noPermiteUsuarioNulo() { assertRegistro(HttpStatus.BAD_GATEWAY); }
    @Test void noPermiteUsuarioConIdDistinto() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(5L,true));
        assertRegistro(HttpStatus.BAD_GATEWAY);
    }
    @Test void usuarioNoEncontradoDa404() {
        when(usuarios.buscarPorId(7L)).thenThrow(mock(FeignException.NotFound.class));
        assertRegistro(HttpStatus.NOT_FOUND);
    }
    @Test void usuarioRemotoCaidoDa503() {
        when(usuarios.buscarPorId(7L)).thenThrow(mock(FeignException.class));
        assertRegistro(HttpStatus.SERVICE_UNAVAILABLE);
    }
    @Test void pagoInexistenteDa404() {
        usuarioActivo();
        Reporte r=datos(); r.setIdPago(23L);
        when(pagos.buscarPorId(23L)).thenThrow(mock(FeignException.NotFound.class));
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            () -> service.registrar(r)).getStatusCode());
    }
    @Test void pagoRemotoCaidoDa503() {
        usuarioActivo();
        Reporte r=datos(); r.setIdPago(23L);
        when(pagos.buscarPorId(23L)).thenThrow(mock(FeignException.class));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ResponseStatusException.class,
            () -> service.registrar(r)).getStatusCode());
    }
    @Test void pagoNuloDa502() {
        usuarioActivo();
        Reporte r=datos(); r.setIdPago(23L);
        assertEquals(HttpStatus.BAD_GATEWAY,assertThrows(ResponseStatusException.class,
            () -> service.registrar(r)).getStatusCode());
    }
    @Test void pagoConIdDistintoDa502() {
        usuarioActivo();
        Reporte r=datos(); r.setIdPago(23L);
        when(pagos.buscarPorId(23L)).thenReturn(new PagosClient.PagoResumen(20L,7L));
        assertEquals(HttpStatus.BAD_GATEWAY,assertThrows(ResponseStatusException.class,
            () -> service.registrar(r)).getStatusCode());
    }
    @Test void pagoDeOtroUsuarioDa400() {
        usuarioActivo();
        Reporte r=datos(); r.setIdPago(23L);
        when(pagos.buscarPorId(23L)).thenReturn(new PagosClient.PagoResumen(23L,8L));
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> service.registrar(r)).getStatusCode());
    }
    @Test void actualizarReporteCompleto() {
        usuarioActivo();
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Reporte r=datos(); r.setAsunto(" Nuevo ");
        assertEquals("Nuevo", service.actualizar(8L,r).getAsunto());
    }
    @Test void actualizarReporteInexistenteDa404() {
        usuarioActivo();
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            () -> service.actualizar(99L,datos())).getStatusCode());
    }
    @Test void actualizarSeguimientoSinDatosDa400() {
        Reporte cambios = new Reporte();
        cambios.setEstado(null);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.actualizarSeguimiento(8L,cambios)).getStatusCode());
    }
    @Test void respuestaDemasiadoLargaDa400() {
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        Reporte cambios=new Reporte(); cambios.setRespuesta("r".repeat(2001));
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> service.actualizarSeguimiento(8L,cambios)).getStatusCode());
    }
    @Test void agenteNoPositivoDa400() {
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        Reporte cambios=new Reporte(); cambios.setIdAgente(0L);
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> service.actualizarSeguimiento(8L,cambios)).getStatusCode());
    }
    @Test void seguimientoSoloConRespuestaEsValido() {
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Reporte cambios=new Reporte(); cambios.setRespuesta(" Resuelto ");
        assertEquals("Resuelto",service.actualizarSeguimiento(8L,cambios).getRespuesta());
    }
    @Test void seguimientoConEstadoPendienteEsValido() {
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Reporte cambios=new Reporte(); cambios.setEstado("pendiente");
        assertEquals("PENDIENTE",service.actualizarSeguimiento(8L,cambios).getEstado());
    }
    @Test void seguimientoConEstadoEnProcesoEsValido() {
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Reporte cambios=new Reporte(); cambios.setEstado("en_proceso");
        assertEquals("EN_PROCESO",service.actualizarSeguimiento(8L,cambios).getEstado());
    }
    void assertRegistro(HttpStatus status) {
        assertEquals(status, assertThrows(ResponseStatusException.class,
            () -> service.registrar(datos())).getStatusCode());
    }
}
