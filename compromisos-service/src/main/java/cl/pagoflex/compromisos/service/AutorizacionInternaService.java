package cl.pagoflex.compromisos.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutorizacionInternaService {

    private final byte[] claveInterna;

    public AutorizacionInternaService(
            @Value("${PAGOFLEX_INTERNAL_SECRET:}") String clave
    ) {
        this.claveInterna = clave.getBytes(StandardCharsets.UTF_8);
    }

    public void verificar(String claveRecibida) {

        if (claveInterna.length < 32) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Autenticacion interna no configurada"
            );
        }

        if (claveRecibida == null
                || !MessageDigest.isEqual(
                        claveInterna,
                        claveRecibida.getBytes(StandardCharsets.UTF_8)
                )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Acceso interno no autorizado"
            );
        }
    }
}