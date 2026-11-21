package cl.pagoflex.reportes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import cl.pagoflex.reportes.model.Reporte;
import cl.pagoflex.reportes.service.ReporteService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reportes", description = "Creación, consulta y seguimiento de incidencias asociadas a usuarios y pagos.")
@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Operation(summary = "Crear reporte",
            description = "Registra una incidencia o consulta de un usuario, opcionalmente asociada a un pago.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reporte creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Usuario o pago no encontrado"),
        @ApiResponse(responseCode = "503", description = "Servicio relacionado no disponible")
    })
    @PostMapping
    public ResponseEntity<Reporte> registrar(
            @RequestBody Reporte datos
    ) {

        Reporte reporte = reporteService.registrar(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reporte);
    }

    @Operation(summary = "Listar reportes",
            description = "Devuelve los reportes almacenados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de reportes")
    })
    @GetMapping
    public ResponseEntity<List<Reporte>> listarTodos() {

        return ResponseEntity.ok(
                reporteService.listarTodos()
        );
    }

    @Operation(summary = "Consultar reporte",
            description = "Obtiene el reporte por su identificador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reporte encontrado"),
        @ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Reporte> buscarPorId(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                reporteService.buscarPorId(id)
        );
    }

    @Operation(summary = "Listar reportes por usuario",
            description = "Filtra los reportes asociados a la cuenta indicada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reportes del usuario")
    })
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Reporte>> listarPorUsuario(
            @Parameter(description = "Identificador del usuario.") @PathVariable Long idUsuario
    ) {

        return ResponseEntity.ok(
                reporteService.listarPorUsuario(idUsuario)
        );
    }

    @Operation(summary = "Listar reportes por agente",
            description = "Filtra los reportes asignados al agente indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reportes asignados al agente")
    })
    @GetMapping("/agente/{idAgente}")
    public ResponseEntity<List<Reporte>> listarPorAgente(
            @Parameter(description = "Identificador del agente.") @PathVariable Long idAgente
    ) {

        return ResponseEntity.ok(
                reporteService.listarPorAgente(idAgente)
        );
    }

    @Operation(summary = "Filtrar reportes por estado",
            description = "Consulta reportes por un estado válido, como PENDIENTE, EN_REVISION o RESUELTO.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reportes del estado indicado"),
        @ApiResponse(responseCode = "400", description = "Estado inválido")
    })
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Reporte>> listarPorEstado(
            @Parameter(description = "Estado por el que se filtran los reportes.") @PathVariable String estado
    ) {

        return ResponseEntity.ok(
                reporteService.listarPorEstado(estado)
        );
    }

    @Operation(summary = "Listar reportes por pago",
            description = "Consulta incidencias vinculadas a un pago.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reportes del pago")
    })
    @GetMapping("/pago/{idPago}")
    public ResponseEntity<List<Reporte>> listarPorPago(
            @Parameter(description = "Identificador del pago.") @PathVariable Long idPago
    ) {

        return ResponseEntity.ok(
                reporteService.listarPorPago(idPago)
        );
    }

    @Operation(summary = "Editar reporte",
            description = "Modifica asunto, tipo, descripción y referencias de un reporte.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reporte actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Reporte no encontrado"),
        @ApiResponse(responseCode = "503", description = "Servicio relacionado no disponible")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Reporte> actualizar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @RequestBody Reporte datos
    ) {

        return ResponseEntity.ok(
                reporteService.actualizar(id, datos)
        );
    }

    @Operation(summary = "Actualizar seguimiento",
            description = "Permite cambiar estado, respuesta o agente responsable de una incidencia.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Seguimiento actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos de seguimiento inválidos"),
        @ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    @PutMapping("/{id}/seguimiento")
    public ResponseEntity<Reporte> actualizarSeguimiento(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @RequestBody Reporte datos
    ) {

        return ResponseEntity.ok(
                reporteService.actualizarSeguimiento(id, datos)
        );
    }

    @Operation(summary = "Desactivar reporte",
            description = "Desactiva lógicamente el reporte conservando su registro.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reporte desactivado"),
        @ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        reporteService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
