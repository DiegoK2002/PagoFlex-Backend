package cl.pagoflex.compromisos.controller;

import cl.pagoflex.compromisos.model.Compromiso;
import cl.pagoflex.compromisos.service.CompromisoService;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/compromisos")
public class PagoInternoController {

    private final CompromisoService compromisoService;

    public PagoInternoController(
            CompromisoService compromisoService
    ) {
        this.compromisoService = compromisoService;
    }

    @PostMapping("/{id}/marcar-pagado")
    public ResponseEntity<Compromiso> marcarComoPagado(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(
                compromisoService.marcarComoPagado(id)
        );
    }
}
