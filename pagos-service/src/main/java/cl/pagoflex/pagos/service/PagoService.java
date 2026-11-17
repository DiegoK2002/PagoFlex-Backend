package cl.pagoflex.pagos.service;

import cl.pagoflex.pagos.client.CompromisosClient;
import cl.pagoflex.pagos.client.UsuariosClient;
import cl.pagoflex.pagos.model.Pago;
import cl.pagoflex.pagos.repository.PagoRepository;

import feign.FeignException;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CompromisosClient compromisosClient;
    private final UsuariosClient usuariosClient;

    public PagoService(
            PagoRepository pagoRepository,
            CompromisosClient compromisosClient,
            UsuariosClient usuariosClient
    ) {
        this.pagoRepository = pagoRepository;
        this.compromisosClient = compromisosClient;
        this.usuariosClient = usuariosClient;
    }

    public Pago registrar(Pago datos) {

        validarDatos(datos);
        validarUsuario(datos.getIdUsuario(), true);
        CompromisosClient.CompromisoResumen compromiso = validarCompromiso(datos);
        validarPagoNuevo(datos, compromiso);

        Pago pago = new Pago();

        pago.setIdUsuario(datos.getIdUsuario());
        pago.setIdCompromiso(datos.getIdCompromiso());
        pago.setMonto(datos.getMonto());
        pago.setFechaPago(datos.getFechaPago());
        pago.setMetodoPago(datos.getMetodoPago().trim());
        pago.setReferenciaComprobante(
                limpiarReferencia(datos.getReferenciaComprobante())
        );
        pago.setActivo(true);
        pago.setEstadoSincronizacion("PENDIENTE");

        Pago registrado = pagoRepository.save(pago);
        return confirmarSincronizacion(registrado);
    }

    public Pago confirmarPendiente(Long idPago) {

        Pago pago = buscarPorId(idPago);

        if (!Boolean.TRUE.equals(pago.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede confirmar un pago inactivo"
            );
        }

        if ("CONFIRMADO".equals(pago.getEstadoSincronizacion())) {
            return pago;
        }

        if (!"PENDIENTE".equals(pago.getEstadoSincronizacion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pago no tiene un estado de sincronizacion valido"
            );
        }

        return confirmarSincronizacion(pago);
    }

    private Pago confirmarSincronizacion(Pago pago) {

        CompromisosClient.CompromisoResumen compromiso;

        try {
            compromiso = compromisosClient.marcarComoPagado(
                    pago.getIdCompromiso()
            );
        } catch (FeignException error) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El pago quedo PENDIENTE: no se pudo confirmar el compromiso",
                    error
            );
        }

        if (compromiso == null
                || !pago.getIdCompromiso().equals(compromiso.idCompromiso())
                || !pago.getIdUsuario().equals(compromiso.idUsuario())
                || !Boolean.TRUE.equals(compromiso.activo())
                || !"PAGADO".equalsIgnoreCase(compromiso.estado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El pago quedo PENDIENTE: respuesta invalida de compromisos-service"
            );
        }

        pago.setEstadoSincronizacion("CONFIRMADO");
        return pagoRepository.save(pago);
    }

    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    public Pago buscarPorId(Long id) {

        return pagoRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Pago no encontrado"
                        )
                );
    }

    public List<Pago> listarPorUsuario(Long idUsuario) {
        return pagoRepository.findByIdUsuario(idUsuario);
    }

    public List<Pago> listarPorCompromiso(Long idCompromiso) {
        return pagoRepository.findByIdCompromiso(idCompromiso);
    }

    public Pago actualizar(Long id, Pago datos) {

        validarDatos(datos);
        validarUsuario(datos.getIdUsuario(), false);
        CompromisosClient.CompromisoResumen compromiso = validarCompromiso(datos);

        Pago pago = buscarPorId(id);
        validarModificacion(datos, pago, compromiso);

        pago.setIdUsuario(datos.getIdUsuario());
        pago.setIdCompromiso(datos.getIdCompromiso());
        pago.setMonto(datos.getMonto());
        pago.setFechaPago(datos.getFechaPago());
        pago.setMetodoPago(datos.getMetodoPago().trim());
        pago.setReferenciaComprobante(
                limpiarReferencia(datos.getReferenciaComprobante())
        );

        return pagoRepository.save(pago);
    }

    public void eliminar(Long id) {

        Pago pago = buscarPorId(id);

        pago.setActivo(false);

        pagoRepository.save(pago);
    }

    private void validarUsuario(Long idUsuario, boolean exigirActivo) {

        UsuariosClient.UsuarioResumen usuario;

        try {

            usuario = usuariosClient.buscarPorId(idUsuario);

        } catch (FeignException.NotFound error) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El usuario asociado no existe"
            );

        } catch (FeignException error) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar usuarios-service",
                    error
            );
        }

        if (usuario == null
                || usuario.idUsuario() == null
                || usuario.activo() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta invalida de usuarios-service"
            );
        }

        if (!usuario.idUsuario().equals(idUsuario)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El usuario consultado no coincide"
            );
        }

        if (exigirActivo && !Boolean.TRUE.equals(usuario.activo())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pueden registrar pagos con usuarios inactivos"
            );
        }
    }

    private CompromisosClient.CompromisoResumen validarCompromiso(Pago datos) {

        CompromisosClient.CompromisoResumen compromiso;

        try {

            compromiso = compromisosClient.buscarPorId(
                    datos.getIdCompromiso()
            );

        } catch (FeignException.NotFound error) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El compromiso asociado no existe"
            );

        } catch (FeignException error) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar compromisos-service",
                    error
            );
        }

        if (compromiso == null
                || compromiso.idCompromiso() == null
                || compromiso.idUsuario() == null
                || compromiso.monto() == null
                || compromiso.estado() == null
                || compromiso.activo() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta invalida de compromisos-service"
            );
        }

        if (!compromiso.idCompromiso().equals(
                datos.getIdCompromiso()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El compromiso consultado no coincide"
            );
        }

        if (!Boolean.TRUE.equals(compromiso.activo())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pueden registrar pagos en compromisos inactivos"
            );
        }

        if (!compromiso.idUsuario().equals(
                datos.getIdUsuario()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El compromiso no pertenece al usuario indicado"
            );
        }

        return compromiso;
    }

    private void validarPagoNuevo(
            Pago datos,
            CompromisosClient.CompromisoResumen compromiso
    ) {
        if (!"PENDIENTE".equalsIgnoreCase(compromiso.estado().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden pagar compromisos pendientes"
            );
        }

        if (datos.getMonto().compareTo(compromiso.monto()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El monto debe coincidir con el compromiso"
            );
        }

        if (pagoRepository.existsByIdCompromisoAndActivoTrue(
                datos.getIdCompromiso()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El compromiso ya tiene un pago registrado"
            );
        }
    }

    private void validarModificacion(
            Pago datos,
            Pago pago,
            CompromisosClient.CompromisoResumen compromiso
    ) {
        if (!Boolean.TRUE.equals(pago.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pueden modificar pagos inactivos"
            );
        }

        if (!pago.getIdUsuario().equals(datos.getIdUsuario())
                || !pago.getIdCompromiso().equals(datos.getIdCompromiso())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede cambiar el usuario ni el compromiso de un pago"
            );
        }

        if (datos.getMonto().compareTo(compromiso.monto()) != 0
                || datos.getMonto().compareTo(pago.getMonto()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede modificar el monto del pago"
            );
        }
    }

    private String limpiarReferencia(String referencia) {

        if (referencia == null || referencia.isBlank()) {
            return null;
        }

        return referencia.trim();
    }

    private void validarDatos(Pago datos) {

        if (datos == null
                || datos.getIdUsuario() == null
                || datos.getIdCompromiso() == null
                || datos.getIdUsuario() <= 0
                || datos.getIdCompromiso() <= 0
                || datos.getMonto() == null
                || datos.getFechaPago() == null
                || datos.getMetodoPago() == null
                || datos.getMetodoPago().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos obligatorios del pago son invalidos"
            );
        }

        if (datos.getMonto().compareTo(BigDecimal.ZERO) <= 0
                || datos.getMonto().precision() > 15
                || datos.getMonto().scale() > 2) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El monto debe ser positivo y tener hasta 2 decimales"
            );
        }

        if (datos.getMetodoPago().trim().length() > 50) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El metodo de pago admite hasta 50 caracteres"
            );
        }

        if (datos.getReferenciaComprobante() != null
                && datos.getReferenciaComprobante().trim().length() > 255) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La referencia admite hasta 255 caracteres"
            );
        }
    }
}
