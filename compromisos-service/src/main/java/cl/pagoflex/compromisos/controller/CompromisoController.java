package cl.pagoflex.compromisos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import cl.pagoflex.compromisos.model.Compromiso;
import cl.pagoflex.compromisos.service.AutorizacionAdministradorService;
import cl.pagoflex.compromisos.service.AutorizacionLecturaService;
import cl.pagoflex.compromisos.service.CompromisoService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Compromisos", description = "Administración y consulta de compromisos con autorización por propietario y rol.")
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@SecurityRequirement(name = "basicAuth")
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

    @Operation(summary = "Asignar compromiso",
            description = "Un administrador registra un compromiso para un usuario y una empresa existentes. El estado inicial es PENDIENTE.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compromiso creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciales ausentes o inválidas"),
        @ApiResponse(responseCode = "403", description = "Administrador requerido"),
        @ApiResponse(responseCode = "404", description = "Usuario o empresa no encontrados"),
        @ApiResponse(responseCode = "503", description = "Servicio relacionado no disponible")
    })
    @PostMapping
    public ResponseEntity<Compromiso> registrar(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @RequestBody Compromiso datos
    ) {

        autorizacionAdministradorService.exigirAdministrador(autorizacion);

        Compromiso compromiso = compromisoService.registrar(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(compromiso);
    }

    @Operation(summary = "Listar todos los compromisos",
            description = "Solo el administrador puede consultar la lista completa.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de compromisos"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Administrador requerido")
    })
    @GetMapping
    public ResponseEntity<List<Compromiso>> listarTodos(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        autorizacionAdministradorService.exigirAdministrador(autorizacion);

        return ResponseEntity.ok(
                compromisoService.listarTodos()
        );
    }

    @Operation(summary = "Consultar compromiso",
            description = "El propietario del compromiso o un administrador puede consultar su detalle.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compromiso encontrado"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Compromiso no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Compromiso> buscarPorId(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        var identidad = autorizacionLecturaService.obtenerIdentidad(autorizacion);

        Compromiso compromiso = compromisoService.buscarPorId(id);

        autorizacionLecturaService.exigirPropietarioOAdministrador(
                identidad,
                compromiso.getIdUsuario()
        );

        return ResponseEntity.ok(compromiso);
    }

    @Operation(summary = "Consultar compromisos de un usuario",
            description = "Solo el propietario o un administrador puede listar los compromisos de la cuenta indicada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de compromisos del usuario"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Compromiso>> listarPorUsuario(
            @Parameter(description = "Identificador del usuario.") @PathVariable Long idUsuario,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
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

    @Operation(summary = "Editar compromiso",
            description = "Solo el administrador puede modificar importe, fecha, empresa, usuario y nombre del compromiso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compromiso actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Administrador requerido"),
        @ApiResponse(responseCode = "404", description = "Compromiso, usuario o empresa no encontrados")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Compromiso> actualizar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @RequestBody Compromiso datos
    ) {

        autorizacionAdministradorService.exigirAdministrador(autorizacion);

        return ResponseEntity.ok(
                compromisoService.actualizar(id, datos)
        );
    }

    @Operation(summary = "Anular compromiso",
            description = "Desactiva el compromiso sin borrar su historial; requiere administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Compromiso anulado"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Administrador requerido"),
        @ApiResponse(responseCode = "404", description = "Compromiso no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        autorizacionAdministradorService.exigirAdministrador(autorizacion);

        compromisoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
