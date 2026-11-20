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
    private final ReporteRepository reportes;
    private final RestClient http = RestClient.create();
    private boolean inicializado;

    public ReportesDemoConfig(ReporteRepository reportes) {
        this.reportes = reportes;
    }

    @Scheduled(initialDelay = 11000, fixedDelay = 20000)
    public synchronized void cargarDatosDeDemostracion() {
        if (inicializado) return;
        if (reportes.count() > 0) {
            inicializado = true;
            return;
        }
        try {
            Map<String, Long> usuarios = usuariosDemo();
            Long idUsuario = usuarios.get("crojas@correo.ejemplo");
            Long idAgente = usuarios.get("alagos@correo.ejemplo");
            Long idAdmin = usuarios.get("admin@correo.ejemplo");
            if (idUsuario == null || idAgente == null || idAdmin == null) return;

            List<Reporte> datos = new ArrayList<>();
            datos.add(reporte(idUsuario, idAgente, "CONSULTA", "DEMO - Consulta pendiente",
                    "Consulta de demostracion sobre vencimientos.", "PENDIENTE", null));
            datos.add(reporte(idUsuario, idAgente, "CONSULTA", "DEMO - Caso en gestion",
                    "Solicitud de demostracion de revision de un compromiso.", "EN_PROCESO",
                    "Caso asignado a agente de demostracion."));
            datos.add(reporte(idUsuario, idAgente, "CONSULTA", "DEMO - Solicitud resuelta",
                    "Consulta de demostracion solucionada.", "RESUELTO",
                    "Gestion concluida correctamente."));
            datos.add(reporte(idAgente, null, "SOPORTE", "DEMO - Caso de alagos@correo.ejemplo",
                    "Caso de demostracion para consultas por rol.", "PENDIENTE", null));
            datos.add(reporte(idAdmin, null, "SOPORTE", "DEMO - Caso de admin@correo.ejemplo",
                    "Caso de demostracion para consultas por rol.", "PENDIENTE", null));
            reportes.saveAll(datos);
            inicializado = true;
            log.info("Datos iniciales: cinco reportes de demostracion creados");
        } catch (Exception ex) {
            log.warn("Datos demo de reportes pendientes; se reintentara: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> usuariosDemo() {
        List<?> respuesta = http.get()
                .uri("http://localhost:8081/api/v1/usuarios")
                .headers(h -> h.setBasicAuth("admin@correo.ejemplo", "DemoPagoFlex2026!"))
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
