package cl.pagoflex.pagos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import cl.pagoflex.pagos.model.Pago;
import cl.pagoflex.pagos.service.PagoService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Pagos", description = "Registro, consulta y administración de pagos y sus compromisos.")
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(summary = "Registrar pago",
            description = "Registra un pago autorizado para su propietario o un administrador y sincroniza el estado del compromiso relacionado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pago registrado y confirmado"),
        @ApiResponse(responseCode = "400", description = "Datos del pago inválidos"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Usuario o compromiso no encontrado"),
        @ApiResponse(responseCode = "409", description = "El compromiso o el pago no admite esta operación"),
        @ApiResponse(responseCode = "503", description = "No se pudo confirmar con el servicio de compromisos")
    })
    @PostMapping
    public ResponseEntity<Pago> registrar(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @RequestBody Pago datos
    ) {

        Pago pago = pagoService.registrar(datos, autorizacion);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pago);
    }

    @Operation(summary = "Listar todos los pagos",
            description = "Consulta el historial completo de pagos; requiere rol administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pagos"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Administrador requerido")
    })
    @GetMapping
    public ResponseEntity<List<Pago>> listarTodos(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        return ResponseEntity.ok(
                pagoService.listarTodosAutorizado(autorizacion)
        );
    }

    @Operation(summary = "Consultar pago",
            description = "Devuelve el pago si la cuenta autenticada es su propietaria o administradora.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago encontrado"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Pago> buscarPorId(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        return ResponseEntity.ok(
                pagoService.buscarPorIdAutorizado(id, autorizacion)
        );
    }

    @Operation(summary = "Listar pagos del usuario",
            description = "Lista los pagos del propietario autenticado o de un administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pagos del usuario"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Pago>> listarPorUsuario(
            @Parameter(description = "Identificador del usuario.") @PathVariable Long idUsuario,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        return ResponseEntity.ok(
                pagoService.listarPorUsuarioAutorizado(idUsuario, autorizacion)
        );
    }

    @Operation(summary = "Listar pagos de un compromiso",
            description = "Devuelve los pagos del compromiso tras comprobar que la cuenta puede consultarlo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagos del compromiso"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Compromiso no encontrado"),
        @ApiResponse(responseCode = "503", description = "Validación remota no disponible")
    })
    @GetMapping("/compromiso/{idCompromiso}")
    public ResponseEntity<List<Pago>> listarPorCompromiso(
            @Parameter(description = "Identificador del compromiso.") @PathVariable Long idCompromiso,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        return ResponseEntity.ok(
                pagoService.listarPorCompromisoAutorizado(
                        idCompromiso,
                        autorizacion
                )
        );
    }

    @Operation(summary = "Actualizar pago",
            description = "Modifica un pago si pertenece al usuario autenticado o a un administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Pago> actualizar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @RequestBody Pago datos
    ) {

        return ResponseEntity.ok(
                pagoService.actualizarAutorizado(id, datos, autorizacion)
        );
    }

    @Operation(summary = "Anular pago",
            description = "Desactiva el pago; esta operación requiere permisos de administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pago anulado"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Administrador requerido"),
        @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String autorizacion
    ) {

        pagoService.eliminarAutorizado(id, autorizacion);

        return ResponseEntity.noContent().build();
    }
}
