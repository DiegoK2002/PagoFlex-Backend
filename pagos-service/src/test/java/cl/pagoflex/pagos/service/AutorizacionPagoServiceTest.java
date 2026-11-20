package cl.pagoflex.pagos.service;

import cl.pagoflex.pagos.client.UsuariosClient;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class AutorizacionPagoServiceTest {
    UsuariosClient cliente;
    AutorizacionPagoService service;
    static final String HEADER="Basic YTpi";
    @BeforeEach void iniciar() {
        cliente=mock(UsuariosClient.class);
        service=new AutorizacionPagoService(cliente);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"Bearer x","Basic ","abc"})
    void cabeceraInvalidaDa401(String h) {
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ResponseStatusException.class,
            () -> service.exigirPropietarioOAdministrador(h,7L)).getStatusCode());
        verifyNoInteractions(cliente);
    }
    @Test void propietarioAutenticadoEsAdmitido() {
        when(cliente.obtenerIdentidad(HEADER)).thenReturn(new UsuariosClient.IdentidadAutenticada(7L,"USUARIO_FINAL"));
        assertDoesNotThrow(() -> service.exigirPropietarioOAdministrador(HEADER,7L));
    }
    @Test void administradorEsAdmitido() {
        when(cliente.obtenerIdentidad(HEADER)).thenReturn(new UsuariosClient.IdentidadAutenticada(5L,"ADMINISTRADOR"));
        assertDoesNotThrow(() -> service.exigirPropietarioOAdministrador(HEADER,null));
    }
    @Test void otroUsuarioDa403() {
        when(cliente.obtenerIdentidad(HEADER)).thenReturn(new UsuariosClient.IdentidadAutenticada(7L,"USUARIO_FINAL"));
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> service.exigirPropietarioOAdministrador(HEADER,8L)).getStatusCode());
    }
    @Test void usuarioNoPuedeConsultarListadoGeneral() {
        when(cliente.obtenerIdentidad(HEADER)).thenReturn(new UsuariosClient.IdentidadAutenticada(7L,"USUARIO_FINAL"));
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> service.exigirPropietarioOAdministrador(HEADER,null)).getStatusCode());
    }
    @Test void identidadNulaDa502() {
        assertEquals(HttpStatus.BAD_GATEWAY,assertThrows(ResponseStatusException.class,
            () -> service.exigirPropietarioOAdministrador(HEADER,7L)).getStatusCode());
    }
    @Test void identidadSinRolDa502() {
        when(cliente.obtenerIdentidad(HEADER)).thenReturn(new UsuariosClient.IdentidadAutenticada(7L,null));
        assertEquals(HttpStatus.BAD_GATEWAY,assertThrows(ResponseStatusException.class,
            () -> service.exigirPropietarioOAdministrador(HEADER,7L)).getStatusCode());
    }
    @Test void credencialesInvalidasDa401() {
        when(cliente.obtenerIdentidad(HEADER)).thenThrow(mock(FeignException.Unauthorized.class));
        assertEstado(HttpStatus.UNAUTHORIZED);
    }
    @Test void sinPermisoRemotoDa403() {
        when(cliente.obtenerIdentidad(HEADER)).thenThrow(mock(FeignException.Forbidden.class));
        assertEstado(HttpStatus.FORBIDDEN);
    }
    @Test void identidadRemotaNoDisponibleDa503() {
        when(cliente.obtenerIdentidad(HEADER)).thenThrow(mock(FeignException.class));
        assertEstado(HttpStatus.SERVICE_UNAVAILABLE);
    }
    void assertEstado(HttpStatus esperado) {
        assertEquals(esperado,assertThrows(ResponseStatusException.class,
            () -> service.exigirPropietarioOAdministrador(HEADER,7L)).getStatusCode());
    }
}
