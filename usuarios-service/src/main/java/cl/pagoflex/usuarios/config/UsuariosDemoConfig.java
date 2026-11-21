package cl.pagoflex.usuarios.config;

import cl.pagoflex.usuarios.model.Rol;
import cl.pagoflex.usuarios.model.Usuario;
import cl.pagoflex.usuarios.repository.RolRepository;
import cl.pagoflex.usuarios.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UsuariosDemoConfig {

    @Bean
    @Order(2)
    public CommandLineRunner inicializarUsuariosDemo(
            UsuarioRepository usuarios,
            RolRepository roles,
            PasswordEncoder codificador
    ) {
        return args -> {
            if (usuarios.count() != 0L) {
                actualizarContrasenasDeDemostracion(usuarios, codificador);
                return;
            }

            Rol rolUsuario = buscarOCrearRol(roles, 1L, "USUARIO_FINAL");
            Rol rolAgente = buscarOCrearRol(roles, 2L, "AGENTE");
            Rol rolAdmin = buscarOCrearRol(roles, 3L, "ADMINISTRADOR");
            String contrasenaDemo = "PagoFlex123";

            guardar(usuarios, codificador, rolUsuario,
                    "99000001-8", "Camila", "Rojas",
                    "crojas@correo.ejemplo", "Santiago", contrasenaDemo);

            guardar(usuarios, codificador, rolAgente,
                    "99000002-6", "Andres", "Lagos",
                    "alagos@correo.ejemplo", "Santiago", contrasenaDemo);

            guardar(usuarios, codificador, rolAdmin,
                    "99000003-4", "Administrador", "PagoFlex",
                    "admin@correo.ejemplo", "Santiago", contrasenaDemo);
        };
    }

    private void actualizarContrasenasDeDemostracion(
            UsuarioRepository usuarios,
            PasswordEncoder codificador
    ) {
        String[] correosDemo = {
                "crojas@correo.ejemplo",
                "alagos@correo.ejemplo",
                "admin@correo.ejemplo"
        };
        for (String correo : correosDemo) {
            usuarios.findByCorreoIgnoreCase(correo).ifPresent(usuario -> {
                if (codificador.matches("DemoPagoFlex2026!", usuario.getContrasenaHash())) {
                    usuario.setContrasenaHash(codificador.encode("PagoFlex123"));
                    usuarios.save(usuario);
                }
            });
        }
    }

    private Rol buscarOCrearRol(RolRepository roles, Long id, String nombre) {
        return roles.findByNombreRol(nombre)
                .orElseGet(() -> {
                    if (roles.existsById(id)) {
                        throw new IllegalStateException(
                                "Identificador de rol ocupado: " + id);
                    }
                    return roles.save(new Rol(id, nombre));
                });
    }

    private void guardar(
            UsuarioRepository usuarios,
            PasswordEncoder codificador,
            Rol rol,
            String rut,
            String nombres,
            String apellidos,
            String correo,
            String comuna,
            String contrasena
    ) {
        Usuario usuario = new Usuario();
        usuario.setRut(rut);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setComuna(comuna);
        usuario.setRol(rol);
        usuario.setContrasenaHash(codificador.encode(contrasena));
        usuario.setActivo(true);
        usuarios.save(usuario);
    }
}
