package cl.pagoflex.usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import cl.pagoflex.usuarios.dto.LoginRequest;
import cl.pagoflex.usuarios.dto.RegistroUsuarioRequest;
import cl.pagoflex.usuarios.dto.UsuarioResponse;
import cl.pagoflex.usuarios.service.UsuarioService;
import cl.pagoflex.usuarios.dto.ActualizarUsuarioRequest;
import cl.pagoflex.usuarios.dto.CambiarContrasenaRequest;
import cl.pagoflex.usuarios.dto.RecuperarContrasenaRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Usuarios y autenticación", description = "Registro, autenticación y administración de cuentas de PagoFlex.")
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Registrar usuario",
            description = "Crea una cuenta de usuario final y valida los campos enviados.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "409", description = "Correo o RUT ya registrado")
    })
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(
            @Valid @RequestBody RegistroUsuarioRequest request
    ) {

        UsuarioResponse usuario = usuarioService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuario);
    }

    @Operation(summary = "Iniciar sesión",
            description = "Valida las credenciales y devuelve los datos de la cuenta.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inicio de sesión correcto"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> iniciarSesion(
            @Valid @RequestBody LoginRequest request
    ) {

        UsuarioResponse usuario = usuarioService.iniciarSesion(request);

        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Consultar usuario por ID",
            description = "Obtiene los datos públicos del usuario identificado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        UsuarioResponse usuario = usuarioService.buscarPorId(id);

        return ResponseEntity.ok(usuario);
    }


    @Operation(summary = "Listar usuarios",
            description = "Devuelve las cuentas registradas. Requiere rol ADMINISTRADOR.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de usuarios"),
        @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
        @ApiResponse(responseCode = "403", description = "Acceso restringido a administradores")
    })
    @GetMapping
    public ResponseEntity<java.util.List<UsuarioResponse>> listarTodos() {

        java.util.List<UsuarioResponse> usuarios =
                usuarioService.listarTodos();

        return ResponseEntity.ok(usuarios);
    }


    @Operation(summary = "Actualizar perfil",
            description = "Modifica los datos de perfil del usuario indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request
    ) {

        UsuarioResponse usuario =
                usuarioService.actualizarUsuario(id, request);

        return ResponseEntity.ok(usuario);
    }


    @Operation(summary = "Desactivar usuario",
            description = "Desactiva la cuenta indicada; no elimina físicamente el registro.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuario desactivado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id
    ) {

        usuarioService.eliminarUsuario(id);

        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Cambiar contraseña",
            description = "Comprueba la contraseña actual y actualiza su hash BCrypt.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Contraseña actualizada"),
        @ApiResponse(responseCode = "400", description = "Contraseña o datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}/contrasena")
    public ResponseEntity<Void> cambiarContrasena(
            @Parameter(description = "Identificador del recurso.") @PathVariable Long id,
            @Valid @RequestBody CambiarContrasenaRequest request
    ) {

        usuarioService.cambiarContrasena(id, request);

        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Recuperar contraseña",
            description = "Actualiza la contraseña utilizando el correo y RUT registrados.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Contraseña restablecida"),
        @ApiResponse(responseCode = "400", description = "Datos de recuperación inválidos"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/recuperar-contrasena")
    public ResponseEntity<Void> recuperarContrasena(
            @Valid @RequestBody RecuperarContrasenaRequest request
    ) {

        usuarioService.recuperarContrasena(request);

        return ResponseEntity.noContent().build();
    }





}
