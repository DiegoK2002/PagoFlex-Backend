package cl.pagoflex.pagos.service;

import cl.pagoflex.pagos.client.*;
import cl.pagoflex.pagos.model.Pago;
import cl.pagoflex.pagos.repository.PagoRepository;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
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


class PagoCoberturaTest {
    PagoRepository repo;
    CompromisosClient compromisos;
    UsuariosClient usuarios;
    AutorizacionPagoService auth;
    PagoService service;
    static final String HEADER="Basic YTpi";
    @BeforeEach void preparar() {
        repo=mock(PagoRepository.class);
        compromisos=mock(CompromisosClient.class);
        usuarios=mock(UsuariosClient.class);
        auth=mock(AutorizacionPagoService.class);
        service=new PagoService(repo,compromisos,usuarios,auth);
    }
    Pago datos() {
        Pago p = new Pago();
        p.setIdPago(8L); p.setIdUsuario(7L); p.setIdCompromiso(32L);
        p.setMonto(new BigDecimal("56295.00"));
        p.setFechaPago(LocalDate.of(2026,11,20));
        p.setMetodoPago(" TRANSFERENCIA ");
        p.setEstadoSincronizacion("PENDIENTE"); p.setActivo(true);
        return p;
    }
    CompromisosClient.CompromisoResumen compromiso(String estado) {
        return new CompromisosClient.CompromisoResumen(32L,7L,new BigDecimal("56295.00"),estado,true);
    }
    void referencias() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenReturn(compromiso("PENDIENTE"));
    }
    static Stream<Pago> invalidos() {
        Pago a=new Pago();
        Pago b=new Pago(); b.setIdUsuario(0L);
        Pago c=new Pago(); c.setIdUsuario(7L); c.setIdCompromiso(0L);
        Pago d=new Pago(); d.setIdUsuario(7L); d.setIdCompromiso(32L); d.setMonto(BigDecimal.ONE);
        Pago e=new Pago(); e.setIdUsuario(7L); e.setIdCompromiso(32L); e.setMonto(BigDecimal.ONE);
        e.setFechaPago(LocalDate.now()); e.setMetodoPago(" ");
        Pago f=new Pago(); f.setIdUsuario(7L); f.setIdCompromiso(32L);
        f.setMonto(new BigDecimal("-2.00")); f.setFechaPago(LocalDate.now()); f.setMetodoPago("EFECTIVO");
        Pago g=new Pago(); g.setIdUsuario(7L); g.setIdCompromiso(32L);
        g.setMonto(new BigDecimal("1.234")); g.setFechaPago(LocalDate.now()); g.setMetodoPago("EFECTIVO");
        Pago h=new Pago(); h.setIdUsuario(7L); h.setIdCompromiso(32L);
        h.setMonto(new BigDecimal("1234567890123456")); h.setFechaPago(LocalDate.now()); h.setMetodoPago("EFECTIVO");
        Pago i=new Pago(); i.setIdUsuario(7L); i.setIdCompromiso(32L);
        i.setMonto(BigDecimal.ONE); i.setFechaPago(LocalDate.now()); i.setMetodoPago("X".repeat(51));
        Pago j=new Pago(); j.setIdUsuario(7L); j.setIdCompromiso(32L);
        j.setMonto(BigDecimal.ONE); j.setFechaPago(LocalDate.now()); j.setMetodoPago("EFECTIVO");
        j.setReferenciaComprobante("X".repeat(256));
        return Stream.of(a,b,c,d,e,f,g,h,i,j);
    }
    @ParameterizedTest @MethodSource("invalidos")
    void noGuardaPagoConCamposInvalidos(Pago p) {
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> service.registrar(p,HEADER)).getStatusCode());
        verify(repo,never()).save(any());
    }
    @Test void registrarRechazaUsuarioInactivo() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,false));
        assertRegistro(HttpStatus.CONFLICT);
    }
    @Test void registrarRechazaUsuarioNulo() { assertRegistro(HttpStatus.BAD_GATEWAY); }
    @Test void registrarRechazaUsuarioIdDiferente() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(8L,true));
        assertRegistro(HttpStatus.BAD_GATEWAY);
    }
    @Test void registrarUsuarioNoEncontrado() {
        when(usuarios.buscarPorId(7L)).thenThrow(mock(FeignException.NotFound.class));
        assertRegistro(HttpStatus.NOT_FOUND);
    }
    @Test void registrarUsuarioRemotoCaido() {
        when(usuarios.buscarPorId(7L)).thenThrow(mock(FeignException.class));
        assertRegistro(HttpStatus.SERVICE_UNAVAILABLE);
    }
    @Test void registrarCompromisoNulo() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        assertRegistro(HttpStatus.BAD_GATEWAY);
    }
    @Test void registrarCompromisoNoEncontrado() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenThrow(mock(FeignException.NotFound.class));
        assertRegistro(HttpStatus.NOT_FOUND);
    }
    @Test void registrarCompromisoRemotoCaido() {
        when(usuarios.buscarPorId(7L)).thenReturn(new UsuariosClient.UsuarioResumen(7L,true));
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenThrow(mock(FeignException.class));
        assertRegistro(HttpStatus.SERVICE_UNAVAILABLE);
    }
    @Test void registrarCompromisoIdDiferente() {
        referencias();
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenReturn(new CompromisosClient.CompromisoResumen(
            33L,7L,BigDecimal.ONE,"PENDIENTE",true));
        assertRegistro(HttpStatus.BAD_GATEWAY);
    }
    @Test void registrarCompromisoInactivo() {
        referencias();
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenReturn(new CompromisosClient.CompromisoResumen(
            32L,7L,BigDecimal.ONE,"PENDIENTE",false));
        assertRegistro(HttpStatus.CONFLICT);
    }
    @Test void registrarCompromisoDeOtroUsuario() {
        referencias();
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenReturn(new CompromisosClient.CompromisoResumen(
            32L,3L,BigDecimal.ONE,"PENDIENTE",true));
        assertRegistro(HttpStatus.BAD_REQUEST);
    }
    @Test void registrarCompromisoPagado() {
        referencias();
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenReturn(compromiso("PAGADO"));
        assertRegistro(HttpStatus.CONFLICT);
    }
    @Test void registrarNoDuplicaPagoActivo() {
        referencias();
        when(repo.existsByIdCompromisoAndActivoTrue(32L)).thenReturn(true);
        assertRegistro(HttpStatus.CONFLICT);
    }
    @Test void registrarConfirmaYNormalizaReferencia() {
        referencias();
        when(compromisos.marcarComoPagado(32L)).thenReturn(compromiso("PAGADO"));
        when(repo.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
        Pago p=datos(); p.setReferenciaComprobante(" REF-1 ");
        var r=service.registrar(p,HEADER);
        assertEquals("CONFIRMADO",r.getEstadoSincronizacion());
        assertEquals("REF-1",r.getReferenciaComprobante());
        assertEquals("TRANSFERENCIA",r.getMetodoPago());
        verify(repo,times(2)).save(any());
    }
    @Test void registrarSinReferenciaDejaNull() {
        referencias();
        when(compromisos.marcarComoPagado(32L)).thenReturn(compromiso("PAGADO"));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var resultado=service.registrar(datos(),HEADER);
        assertNull(resultado.getReferenciaComprobante());
    }
    @Test void confirmacionNoDisponibleProduce503() {
        referencias();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(compromisos.marcarComoPagado(32L)).thenThrow(mock(FeignException.class));
        assertRegistro(HttpStatus.SERVICE_UNAVAILABLE);
        verify(repo,times(1)).save(any());
    }
    @Test void confirmacionConRespuestaInvalidaProduce502() {
        referencias();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertRegistro(HttpStatus.BAD_GATEWAY);
    }
    @Test void pagoConfirmadoNoVuelveAConfirmarse() {
        Pago p=datos(); p.setEstadoSincronizacion("CONFIRMADO");
        when(repo.findById(8L)).thenReturn(Optional.of(p));
        assertSame(p,service.confirmarPendiente(8L));
        verifyNoInteractions(compromisos);
    }
    @Test void pagoInactivoNoSeConfirma() {
        Pago p=datos(); p.setActivo(false);
        when(repo.findById(8L)).thenReturn(Optional.of(p));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> service.confirmarPendiente(8L)).getStatusCode());
    }
    @Test void estadoDeSincronizacionDesconocidoDaConflicto() {
        Pago p=datos(); p.setEstadoSincronizacion("OTRO");
        when(repo.findById(8L)).thenReturn(Optional.of(p));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> service.confirmarPendiente(8L)).getStatusCode());
    }
    @Test void buscarPagoInexistenteDa404() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.buscarPorId(90L)).getStatusCode());
    }
    @Test void listaTodosAutorizadaConsultaRepositorio() {
        when(repo.findAll()).thenReturn(List.of(datos()));
        assertEquals(1,service.listarTodosAutorizado(HEADER).size());
        verify(auth).exigirPropietarioOAdministrador(HEADER,null);
    }
    @Test void listaPorUsuarioAutorizadaVerificaIdentidad() {
        when(repo.findByIdUsuario(7L)).thenReturn(List.of(datos()));
        assertEquals(1,service.listarPorUsuarioAutorizado(7L,HEADER).size());
        verify(auth).exigirPropietarioOAdministrador(HEADER,7L);
    }
    @Test void buscarPorIdAutorizadoVerificaPropietarioReal() {
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        assertSame(service.buscarPorId(8L),service.buscarPorIdAutorizado(8L,HEADER));
        verify(auth).exigirPropietarioOAdministrador(HEADER,7L);
    }
    @Test void listarPorCompromisoSinCredencialesDa401() {
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ResponseStatusException.class,
            () -> service.listarPorCompromisoAutorizado(32L,null)).getStatusCode());
        verifyNoInteractions(compromisos);
    }
    @Test void listarPorCompromisoPropioDevuelveDatos() {
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenReturn(compromiso("PAGADO"));
        when(repo.findByIdCompromiso(32L)).thenReturn(List.of(datos()));
        assertEquals(1,service.listarPorCompromisoAutorizado(32L,HEADER).size());
        verify(auth).exigirPropietarioOAdministrador(HEADER,7L);
    }
    @Test void listarPorCompromisoConRespuestaNulaDa502() {
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ResponseStatusException.class,
            () -> service.listarPorCompromisoAutorizado(32L,HEADER)).getStatusCode());
    }
    @Test void listarPorCompromisoRemoto401() {
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenThrow(mock(FeignException.Unauthorized.class));
        assertListadoStatus(HttpStatus.UNAUTHORIZED);
    }
    @Test void listarPorCompromisoRemoto403() {
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenThrow(mock(FeignException.Forbidden.class));
        assertListadoStatus(HttpStatus.FORBIDDEN);
    }
    @Test void listarPorCompromisoRemoto404() {
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenThrow(mock(FeignException.NotFound.class));
        assertListadoStatus(HttpStatus.NOT_FOUND);
    }
    @Test void listarPorCompromisoRemoto503() {
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenThrow(mock(FeignException.class));
        assertListadoStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }
    @Test void listarPorCompromisoRespuestaConIdIncorrecto() {
        when(compromisos.buscarPorIdAutenticado(32L,HEADER)).thenReturn(new CompromisosClient.CompromisoResumen(
            33L,7L,BigDecimal.ONE,"PAGADO",true));
        assertListadoStatus(HttpStatus.BAD_GATEWAY);
    }
    @Test void actualizacionRechazaPagoInactivo() {
        referencias();
        Pago previo=datos(); previo.setActivo(false);
        when(repo.findById(8L)).thenReturn(Optional.of(previo));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> service.actualizarAutorizado(8L,datos(),HEADER)).getStatusCode());
    }
    @Test void actualizacionRechazaCambioDePropietario() {
        referencias();
        Pago previo=datos(); previo.setIdUsuario(9L);
        when(repo.findById(8L)).thenReturn(Optional.of(previo));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> service.actualizarAutorizado(8L,datos(),HEADER)).getStatusCode());
        verify(repo,never()).save(any());
    }
    @Test void actualizacionRechazaCambioDeMonto() {
        referencias();
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        Pago cambios=datos(); cambios.setMonto(new BigDecimal("100.00"));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.actualizarAutorizado(8L,cambios,HEADER)).getStatusCode());
    }
    @Test void actualizacionExitoso() {
        referencias();
        when(repo.findById(8L)).thenReturn(Optional.of(datos()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Pago cambios=datos(); cambios.setReferenciaComprobante(" respaldo-1 ");
        var actualizado=service.actualizarAutorizado(8L,cambios,HEADER);
        assertEquals("respaldo-1",actualizado.getReferenciaComprobante());
        verify(auth,times(2)).exigirPropietarioOAdministrador(HEADER,7L);
    }
    @Test void eliminarAutorizadoDesactivaPago() {
        Pago previo=datos(); when(repo.findById(8L)).thenReturn(Optional.of(previo));
        service.eliminarAutorizado(8L,HEADER);
        assertFalse(previo.getActivo());
        verify(auth).exigirPropietarioOAdministrador(HEADER,null);
        verify(repo).save(previo);
    }
    void assertRegistro(HttpStatus status) {
        assertEquals(status,assertThrows(ResponseStatusException.class,
            () -> service.registrar(datos(),HEADER)).getStatusCode());
    }
    void assertListadoStatus(HttpStatus status) {
        assertEquals(status,assertThrows(ResponseStatusException.class,
            () -> service.listarPorCompromisoAutorizado(32L,HEADER)).getStatusCode());
    }
}
