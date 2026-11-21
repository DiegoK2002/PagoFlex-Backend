package cl.pagoflex.reportes.config;

import cl.pagoflex.reportes.model.Reporte;
import cl.pagoflex.reportes.repository.ReporteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableScheduling
public class ReportesDemoConfig {

    private static final Logger log = LoggerFactory.getLogger(ReportesDemoConfig.class);

    private static final Map<String, String> ASUNTOS_ANTIGUOS = Map.of(
            "DEMO - Consulta pendiente", "Consulta por fecha de vencimiento",
            "DEMO - Caso en gestion", "Revisión de monto facturado",
            "DEMO - Solicitud resuelta", "Confirmación de pago registrado",
            "DEMO - Caso de alagos@correo.ejemplo", "Problema para consultar historial",
            "DEMO - Caso de admin@correo.ejemplo", "Actualización de información comercial"
    );

    private final ReporteRepository reportes;
    private final RestClient http = RestClient.create();
    private boolean inicializado;

    public ReportesDemoConfig(ReporteRepository reportes) {
        this.reportes = reportes;
    }

    @Scheduled(initialDelay = 11000, fixedDelay = 20000)
    public synchronized void cargarDatosDeDemostracion() {
        if (inicializado) return;
        try {
            if (reportes.count() > 0) {
                actualizarReportesIniciales();
                inicializado = true;
                return;
            }

            Map<String, Long> usuarios = usuariosDemo();
            Long idUsuario = usuarios.get("crojas@correo.ejemplo");
            Long idAgente = usuarios.get("alagos@correo.ejemplo");
            Long idAdmin = usuarios.get("admin@correo.ejemplo");
            if (idUsuario == null || idAgente == null || idAdmin == null) return;

            List<Reporte> datos = new ArrayList<>();
            datos.add(reporte(idUsuario, idAgente, "CONSULTA", "Consulta por fecha de vencimiento",
                    "Quisiera confirmar la fecha de vencimiento de mi cuenta de electricidad.", "PENDIENTE", null));
            datos.add(reporte(idUsuario, idAgente, "CONSULTA", "Revisión de monto facturado",
                    "El importe de la última factura no coincide con el monto informado.", "EN_PROCESO",
                    "Solicitud recibida; estamos revisando los antecedentes."));
            datos.add(reporte(idUsuario, idAgente, "CONSULTA", "Confirmación de pago registrado",
                    "Realicé una transferencia y solicito verificar su registro.", "RESUELTO",
                    "El pago fue confirmado y el compromiso quedó actualizado."));
            datos.add(reporte(idAgente, null, "SOPORTE", "Problema para consultar historial",
                    "No se visualiza el detalle de algunos movimientos en el historial.", "PENDIENTE", null));
            datos.add(reporte(idAdmin, null, "SOPORTE", "Actualización de información comercial",
                    "Se solicita revisar los datos de contacto de una empresa registrada.", "PENDIENTE", null));
            reportes.saveAll(datos);
            inicializado = true;
            log.info("Cinco reportes iniciales creados");
        } catch (Exception ex) {
            log.warn("Inicialización de reportes pendiente: {}", ex.getMessage());
        }
    }

    private void actualizarReportesIniciales() {
        for (Reporte reporte : reportes.findAll()) {
            String asuntoOriginal = reporte.getAsunto();
            String nuevoAsunto = ASUNTOS_ANTIGUOS.get(asuntoOriginal);
            if (nuevoAsunto == null) continue;
            reporte.setAsunto(nuevoAsunto);
            switch (asuntoOriginal) {
                case "DEMO - Consulta pendiente" -> {
                    actualizarDescripcionSiCoincide(reporte,
                            "Consulta de demostracion sobre vencimientos.",
                            "Consulta de prueba sobre vencimientos.",
                            "Quisiera confirmar la fecha de vencimiento de mi cuenta de electricidad.");
                }
                case "DEMO - Caso en gestion" -> {
                    actualizarDescripcionSiCoincide(reporte,
                            "Solicitud de demostracion de revision de un compromiso.",
                            "Solicitud ficticia de revision de un compromiso.",
                            "El importe de la última factura no coincide con el monto informado.");
                    if ("Caso asignado a agente de demostracion.".equals(reporte.getRespuesta())) {
                        reporte.setRespuesta("Solicitud recibida; estamos revisando los antecedentes.");
                    }
                }
                case "DEMO - Solicitud resuelta" -> {
                    actualizarDescripcionSiCoincide(reporte,
                            "Consulta de demostracion solucionada.",
                            "Consulta de prueba solucionada.",
                            "Realicé una transferencia y solicito verificar su registro.");
                    if ("Gestion concluida correctamente.".equals(reporte.getRespuesta())) {
                        reporte.setRespuesta("El pago fue confirmado y el compromiso quedó actualizado.");
                    }
                }
                default -> actualizarDescripcionSiCoincide(reporte,
                        "Caso de demostracion para consultas por rol.",
                        "Caso ficticio para mostrar la consulta por rol.",
                        "Se solicita revisar información relacionada con la cuenta.");
            }
            reportes.save(reporte);
        }
    }

    private void actualizarDescripcionSiCoincide(Reporte reporte, String anterior1,
                                                   String anterior2, String nuevo) {
        String descripcion = reporte.getDescripcion();
        if (anterior1.equals(descripcion) || anterior2.equals(descripcion)) {
            reporte.setDescripcion(nuevo);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> usuariosDemo() {
        List<?> respuesta = http.get()
                .uri("http://localhost:8081/api/v1/usuarios")
                .headers(h -> h.setBasicAuth("admin@correo.ejemplo", "PagoFlex123"))
                .retrieve()
                .body(List.class);
        Map<String, Long> resultado = new HashMap<>();
        if (respuesta == null) return resultado;
        for (Object elemento : respuesta) {
            Map<String, Object> usuario = (Map<String, Object>) elemento;
            resultado.put(String.valueOf(usuario.get("correo")),
                    ((Number) usuario.get("idUsuario")).longValue());
        }
        return resultado;
    }

    private Reporte reporte(Long usuario, Long agente, String tipo, String asunto,
                            String descripcion, String estado, String respuesta) {
        Reporte reporte = new Reporte();
        reporte.setIdUsuario(usuario);
        reporte.setIdAgente(agente);
        reporte.setTipo(tipo);
        reporte.setAsunto(asunto);
        reporte.setDescripcion(descripcion);
        reporte.setEstado(estado);
        reporte.setRespuesta(respuesta);
        reporte.setActivo(true);
        return reporte;
    }
}
