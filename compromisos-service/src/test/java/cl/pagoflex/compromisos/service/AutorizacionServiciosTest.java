package cl.pagoflex.compromisos.service;

import cl.pagoflex.compromisos.client.UsuariosClient;
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


class AutorizacionServiciosTest {
    UsuariosClient cliente;
    AutorizacionAdministradorService administracion;
    AutorizacionLecturaService lectura;
    static final String BASIC = "Basic dGVzdDp0ZXN0";
    @BeforeEach void iniciar() {
        cliente = mock(UsuariosClient.class);
        administracion = new AutorizacionAdministradorService(cliente);
        lectura = new AutorizacionLecturaService(cliente);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"Bearer token","Basic ","x"})
    void administradorRechazaCabecerasInvalidas(String credenciales) {
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
            () -> administracion.exigirAdministrador(credenciales)).getStatusCode());
        verifyNoInteractions(cliente);
    }
    @Test void administradorAceptaCredencialesValidadas() {
        assertDoesNotThrow(() -> administracion.exigirAdministrador(BASIC));
        verify(cliente).verificarAdministrador(BASIC);
    }
    @Test void administradorTraduce401() {
        doThrow(mock(FeignException.Unauthorized.class)).when(cliente).verificarAdministrador(BASIC);
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ResponseStatusException.class,
            () -> administracion.exigirAdministrador(BASIC)).getStatusCode());
    }
    @Test void administradorTraduce403() {
        doThrow(mock(FeignException.Forbidden.class)).when(cliente).verificarAdministrador(BASIC);
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> administracion.exigirAdministrador(BASIC)).getStatusCode());
    }
    @Test void administradorTraduceFalloRemoto() {
        doThrow(mock(FeignException.class)).when(cliente).verificarAdministrador(BASIC);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ResponseStatusException.class,
            () -> administracion.exigirAdministrador(BASIC)).getStatusCode());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"Bearer x","Basic ","otro"})
    void identidadRechazaCredencialesInvalidas(String cabecera) {
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ResponseStatusException.class,
            () -> lectura.obtenerIdentidad(cabecera)).getStatusCode());
        verifyNoInteractions(cliente);
    }
    @Test void obtieneIdentidad() {
        var usuario = new UsuariosClient.IdentidadAutenticada(7L,"USUARIO_FINAL");
        when(cliente.obtenerIdentidad(BASIC)).thenReturn(usuario);
        assertSame(usuario, lectura.obtenerIdentidad(BASIC));
    }
    @Test void obtenerIdentidadTraduce401() {
        when(cliente.obtenerIdentidad(BASIC)).thenThrow(mock(FeignException.Unauthorized.class));
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ResponseStatusException.class,
            () -> lectura.obtenerIdentidad(BASIC)).getStatusCode());
    }
    @Test void obtenerIdentidadTraduce403() {
        when(cliente.obtenerIdentidad(BASIC)).thenThrow(mock(FeignException.Forbidden.class));
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> lectura.obtenerIdentidad(BASIC)).getStatusCode());
    }
    @Test void obtenerIdentidadTraduce503() {
        when(cliente.obtenerIdentidad(BASIC)).thenThrow(mock(FeignException.class));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ResponseStatusException.class,
            () -> lectura.obtenerIdentidad(BASIC)).getStatusCode());
    }
    @Test void identidadNulaProduce502() {
        assertEquals(HttpStatus.BAD_GATEWAY,assertThrows(ResponseStatusException.class,
            () -> lectura.obtenerIdentidad(BASIC)).getStatusCode());
    }
    @Test void identidadSinRolProduce502() {
        when(cliente.obtenerIdentidad(BASIC)).thenReturn(new UsuariosClient.IdentidadAutenticada(7L,null));
        assertEquals(HttpStatus.BAD_GATEWAY,assertThrows(ResponseStatusException.class,
            () -> lectura.obtenerIdentidad(BASIC)).getStatusCode());
    }
    @Test void propietarioPuedeLeer() {
        var identidad = new UsuariosClient.IdentidadAutenticada(7L,"USUARIO_FINAL");
        assertDoesNotThrow(() -> lectura.exigirPropietarioOAdministrador(identidad,7L));
    }
    @Test void administradorPuedeLeerCualquierUsuario() {
        var identidad = new UsuariosClient.IdentidadAutenticada(5L,"ADMINISTRADOR");
        assertDoesNotThrow(() -> lectura.exigirPropietarioOAdministrador(identidad,7L));
    }
    @Test void otroUsuarioNoPuedeLeer() {
        var identidad = new UsuariosClient.IdentidadAutenticada(7L,"USUARIO_FINAL");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> lectura.exigirPropietarioOAdministrador(identidad,8L)).getStatusCode());
    }
    @Test void identificadorNuloNoPermiteLeer() {
        var identidad = new UsuariosClient.IdentidadAutenticada(7L,"USUARIO_FINAL");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            () -> lectura.exigirPropietarioOAdministrador(identidad,null)).getStatusCode());
    }
    @Test void validaClaveInternaCorrecta() {
        var service = new AutorizacionInternaService("S".repeat(48));
        assertDoesNotThrow(() -> service.verificar("S".repeat(48)));
    }
    @Test void claveInternaNoConfiguradaProduce503() {
        var service = new AutorizacionInternaService("");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ResponseStatusException.class,
            () -> service.verificar("abc")).getStatusCode());
    }
    @Test void claveInternaIncorrectaProduce401() {
        var service = new AutorizacionInternaService("S".repeat(48));
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ResponseStatusException.class,
            () -> service.verificar("X".repeat(48))).getStatusCode());
    }
    @Test void claveInternaNulaProduce401() {
        var service = new AutorizacionInternaService("S".repeat(48));
        assertEquals(HttpStatus.UNAUTHORIZED,assertThrows(ResponseStatusException.class,
            () -> service.verificar(null)).getStatusCode());
    }
}
