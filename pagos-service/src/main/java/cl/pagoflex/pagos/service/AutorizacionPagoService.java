package cl.pagoflex.pagos.service;

import cl.pagoflex.pagos.client.UsuariosClient;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutorizacionPagoService {

    private final UsuariosClient usuariosClient;

    public AutorizacionPagoService(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    public void exigirPropietarioOAdministrador(
            String autorizacion,
            Long idUsuario
    ) {

        if (autorizacion == null
                || !autorizacion.startsWith("Basic ")
                || autorizacion.substring(6).isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Autenticacion requerida"
            );
        }

        UsuariosClient.IdentidadAutenticada identidad;

        try {
            identidad = usuariosClient.obtenerIdentidad(autorizacion);
        } catch (FeignException.Unauthorized error) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales invalidas"
            );
        } catch (FeignException.Forbidden error) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso denegado"
            );
        } catch (FeignException error) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar la identidad"
            );
        }

        if (identidad == null
                || identidad.idUsuario() == null
                || identidad.rol() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta de identidad invalida"
            );
        }

        if ("ADMINISTRADOR".equals(identidad.rol())) {
            return;
        }

        if (idUsuario != null
                && idUsuario.equals(identidad.idUsuario())) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puede registrar pagos de otro usuario"
        );
    }
}