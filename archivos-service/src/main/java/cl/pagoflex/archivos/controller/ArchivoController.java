package cl.pagoflex.archivos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import cl.pagoflex.archivos.model.Archivo;
import cl.pagoflex.archivos.service.ArchivoService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Archivos y comprobantes", description = "Carga, metadatos, descarga y desactivación de archivos de usuarios y pagos.")
@RestController
@RequestMapping("/api/v1/archivos")
public class ArchivoController {

    private final ArchivoService archivoService;

    public ArchivoController(ArchivoService archivoService) {
        this.archivoService = archivoService;
    }

    @Operation(summary = "Subir archivo",
            description = "Carga un archivo multipart de hasta 10 MB y lo asocia a un usuario y opcionalmente a un pago. Valida nombre, formato y contenido.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Archivo almacenado"),
        @ApiResponse(responseCode = "400", description = "Archivo o datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Usuario o pago no encontrado"),
        @ApiResponse(responseCode = "503", description = "Servicio relacionado no disponible")
    })
    @PostMapping(
            value = "/subir",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Archivo> subir(
            @Parameter(description = "Identificador del usuario.") @RequestParam Long idUsuario,
            @Parameter(description = "Identificador del pago.") @RequestParam(required = false) Long idPago,
            @Parameter(description = "Tipo de archivo que se desea consultar o registrar.") @RequestParam String tipo,
            @Parameter(description = "Archivo que se adjunta en la solicitud multipart.") @RequestPart("archivo") MultipartFile archivo
    ) {

        Archivo registro = archivoService.subir(
                idUsuario,
                idPago,
                tipo,
                archivo
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(registro);
    }

    @Operation(summary = "Listar archivos",
            description = "Consulta los metadatos de los archivos registrados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de archivos")
    })
    @GetMapping
    public ResponseEntity<List<Archivo>> listarTodos() {

        return ResponseEntity.ok(
                archivoService.listarTodos()
        );
    }

    @Operation(summary = "Consultar archivo",
            description = "Consulta los metadatos de un archivo por ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivo encontrado"),
        @ApiResponse(responseCode = "404", description = "Archivo no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Archivo> buscarPorId(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                archivoService.buscarPorId(id)
        );
    }

    @Operation(summary = "Listar archivos por usuario",
            description = "Consulta los archivos asociados al usuario indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivos del usuario")
    })
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Archivo>> listarPorUsuario(
            @Parameter(description = "Identificador del usuario.") @PathVariable Long idUsuario
    ) {

        return ResponseEntity.ok(
                archivoService.listarPorUsuario(idUsuario)
        );
    }

    @Operation(summary = "Listar archivos por pago",
            description = "Consulta los archivos asociados al pago indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivos del pago")
    })
    @GetMapping("/pago/{idPago}")
    public ResponseEntity<List<Archivo>> listarPorPago(
            @Parameter(description = "Identificador del pago.") @PathVariable Long idPago
    ) {

        return ResponseEntity.ok(
                archivoService.listarPorPago(idPago)
        );
    }

    @Operation(summary = "Listar archivos por tipo",
            description = "Filtra los archivos mediante su tipo de uso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivos del tipo indicado"),
        @ApiResponse(responseCode = "400", description = "Tipo inválido")
    })
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Archivo>> listarPorTipo(
            @Parameter(description = "Tipo de archivo que se desea consultar o registrar.") @PathVariable String tipo
    ) {

        return ResponseEntity.ok(
                archivoService.listarPorTipo(tipo)
        );
    }

    @Operation(summary = "Descargar archivo",
            description = "Devuelve el contenido binario como adjunto con su tipo MIME y nombre original.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Descarga del archivo"),
        @ApiResponse(responseCode = "404", description = "Archivo no encontrado o no disponible")
    })
    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        Archivo archivo = archivoService.buscarPorId(id);

        Resource recurso = archivoService.descargar(id);

        ContentDisposition disposicion = ContentDisposition
                .attachment()
                .filename(
                        archivo.getNombreOriginal(),
                        StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposicion.toString()
                )
                .contentType(
                        MediaType.parseMediaType(
                                archivo.getTipoMime()
                        )
                )
                .contentLength(archivo.getTamanoBytes())
                .body(recurso);
    }

    @Operation(summary = "Desactivar archivo",
            description = "Marca el registro como inactivo; esta operación no borra necesariamente el archivo físico.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Archivo desactivado"),
        @ApiResponse(responseCode = "404", description = "Archivo no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        archivoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
