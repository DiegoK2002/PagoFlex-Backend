package cl.pagoflex.usuarios.service;

import cl.pagoflex.usuarios.model.Usuario;
import cl.pagoflex.usuarios.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetallesService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetallesService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository
                .findByCorreoIgnoreCase(correo)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado"
                        )
                );

        String rol = usuario.getRol().getNombreRol();

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getContrasenaHash())
                .authorities("ROLE_" + rol)
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}