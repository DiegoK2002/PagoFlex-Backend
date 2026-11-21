package cl.pagoflex.empresas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import cl.pagoflex.empresas.model.Empresa;
import cl.pagoflex.empresas.service.EmpresaService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Empresas", description = "Consulta y mantenimiento de empresas asociadas a compromisos.")
@RestController
@RequestMapping("/api/v1/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @Operation(summary = "Crear empresa",
            description = "Registra una empresa con nombre y categoría.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Empresa registrada"),
        @ApiResponse(responseCode = "400", description = "Nombre o categoría inválidos")
    })
    @PostMapping
    public ResponseEntity<Empresa> registrar(
            @RequestBody Empresa datos
    ) {

        Empresa empresa = empresaService.registrar(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(empresa);
    }

    @Operation(summary = "Listar empresas",
            description = "Consulta todas las empresas registradas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de empresas")
    })
    @GetMapping
    public ResponseEntity<List<Empresa>> listarTodas() {

        return ResponseEntity.ok(
                empresaService.listarTodas()
        );
    }

    @Operation(summary = "Consultar empresa",
            description = "Obtiene la empresa correspondiente al identificador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Empresa> buscarPorId(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                empresaService.buscarPorId(id)
        );
    }

    @Operation(summary = "Actualizar empresa",
            description = "Modifica el nombre y la categoría de una empresa existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Empresa actualizada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Empresa> actualizar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @RequestBody Empresa datos
    ) {

        return ResponseEntity.ok(
                empresaService.actualizar(id, datos)
        );
    }

    @Operation(summary = "Desactivar empresa",
            description = "Marca una empresa como inactiva sin eliminar su registro.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Empresa desactivada"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        empresaService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
