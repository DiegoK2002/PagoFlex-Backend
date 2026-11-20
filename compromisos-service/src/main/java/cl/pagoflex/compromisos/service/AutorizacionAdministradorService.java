package cl.pagoflex.compromisos.service;

import cl.pagoflex.compromisos.client.UsuariosClient;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutorizacionAdministradorService {

    private final UsuariosClient usuariosClient;

    public AutorizacionAdministradorService(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    public void exigirAdministrador(String autorizacion) {

        if (autorizacion == null
                || !autorizacion.startsWith("Basic ")
                || autorizacion.substring(6).isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Autenticacion requerida"
            );
        }

        try {
            usuariosClient.verificarAdministrador(autorizacion);
        } catch (FeignException.Unauthorized error) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales invalidas"
            );
        } catch (FeignException.Forbidden error) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Permisos de administrador requeridos"
            );
        } catch (FeignException error) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar la autorizacion"
            );
        }
    }
}