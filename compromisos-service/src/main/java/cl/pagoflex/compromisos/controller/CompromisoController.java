package cl.pagoflex.compromisos.controller;

import cl.pagoflex.compromisos.model.Compromiso;
import cl.pagoflex.compromisos.service.AutorizacionAdministradorService;
import cl.pagoflex.compromisos.service.AutorizacionLecturaService;
import cl.pagoflex.compromisos.service.CompromisoService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compromisos")
public class CompromisoController {

    private final CompromisoService compromisoService;
    private final AutorizacionAdministradorService autorizacionAdministradorService;
    private final AutorizacionLecturaService autorizacionLecturaService;

    public CompromisoController(
            CompromisoService compromisoService,
            AutorizacionAdministradorService autorizacionAdministradorService,
            AutorizacionLecturaService autorizacionLecturaService
    ) {
        this.compromisoService = compromisoService;
        this.autorizacionAdministradorService = autorizacionAdministradorService;
        this.autorizacionLecturaService = autorizacionLecturaService;
    }

    @PostMapping
    public ResponseEntity<Compromiso> registrar(
            @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @RequestBody Compromiso datos
    ) {

        autorizacionAdministradorService.exigirAdministrador(autorizacion);

        Compromiso compromiso = compromisoService.registrar(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(compromiso);
    }

    @GetMapping
    public ResponseEntity<List<Compromiso>> listarTodos(
            @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        autorizacionAdministradorService.exigirAdministrador(autorizacion);

        return ResponseEntity.ok(
                compromisoService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compromiso> buscarPorId(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        var identidad = autorizacionLecturaService.obtenerIdentidad(autorizacion);

        Compromiso compromiso = compromisoService.buscarPorId(id);

        autorizacionLecturaService.exigirPropietarioOAdministrador(
                identidad,
                compromiso.getIdUsuario()
        );

        return ResponseEntity.ok(compromiso);
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Compromiso>> listarPorUsuario(
            @PathVariable Long idUsuario,
            @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        var identidad = autorizacionLecturaService.obtenerIdentidad(autorizacion);

        autorizacionLecturaService.exigirPropietarioOAdministrador(
                identidad,
                idUsuario
        );

        return ResponseEntity.ok(
                compromisoService.listarPorUsuario(idUsuario)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Compromiso> actualizar(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @RequestBody Compromiso datos
    ) {

        autorizacionAdministradorService.exigirAdministrador(autorizacion);

        return ResponseEntity.ok(
                compromisoService.actualizar(id, datos)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        autorizacionAdministradorService.exigirAdministrador(autorizacion);

        compromisoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
