package cl.pagoflex.compromisos.service;

import cl.pagoflex.compromisos.client.UsuariosClient;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutorizacionLecturaService {

    private final UsuariosClient usuariosClient;

    public AutorizacionLecturaService(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    public UsuariosClient.IdentidadAutenticada obtenerIdentidad(
            String autorizacion
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

        return identidad;
    }

    public void exigirPropietarioOAdministrador(
            UsuariosClient.IdentidadAutenticada identidad,
            Long idUsuario
    ) {

        if ("ADMINISTRADOR".equals(identidad.rol())) {
            return;
        }

        if (idUsuario != null
                && idUsuario.equals(identidad.idUsuario())) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puede consultar compromisos de otro usuario"
        );
    }
}