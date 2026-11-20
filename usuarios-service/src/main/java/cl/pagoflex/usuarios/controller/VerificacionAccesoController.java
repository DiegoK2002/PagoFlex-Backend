package cl.pagoflex.usuarios.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class VerificacionAccesoController {

    @GetMapping("/verificar-administrador")
    public ResponseEntity<Void> verificarAdministrador() {
        return ResponseEntity.noContent().build();
    }
}