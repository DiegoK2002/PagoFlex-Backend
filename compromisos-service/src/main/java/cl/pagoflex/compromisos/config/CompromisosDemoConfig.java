package cl.pagoflex.compromisos.config;

import cl.pagoflex.compromisos.model.Compromiso;
import cl.pagoflex.compromisos.repository.CompromisoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableScheduling
public class CompromisosDemoConfig {

    private static final Logger log = LoggerFactory.getLogger(CompromisosDemoConfig.class);
    private static final String ADMIN = "admin@correo.ejemplo";
    private static final String CLAVE = "DemoPagoFlex2026!";
    private final CompromisoRepository compromisos;
    private final RestClient http = RestClient.create();
    private boolean inicializado;

    public CompromisosDemoConfig(CompromisoRepository compromisos) {
        this.compromisos = compromisos;
    }

    @Scheduled(initialDelay = 9000, fixedDelay = 20000)
    public synchronized void cargarDatosDeDemostracion() {
        if (inicializado) return;
        if (compromisos.count() > 0) {
            inicializado = true;
            return;
        }
        try {
            Map<String, Object> usuario = encontrarPorTexto(
                    obtener("http://localhost:8081/api/v1/usuarios", true),
                    "correo", "crojas@correo.ejemplo");
            if (usuario == null) return;
            long idUsuario = numero(usuario, "idUsuario");

            Map<String, Long> empresas = new HashMap<>();
            for (Map<String, Object> empresa : obtener("http://localhost:8082/api/v1/empresas", false)) {
                empresas.put(String.valueOf(empresa.get("nombre")), numero(empresa, "idEmpresa"));
            }
            String[] nombresEmpresas = {
                    "Agua Cordillera Demo", "Energia Central Demo", "Fibra Ciudad Demo",
                    "Comercio Vecinal Demo", "Servicios Urbanos Demo"
            };
            for (String nombre : nombresEmpresas) if (!empresas.containsKey(nombre)) return;

            String[] etiquetas = {"PAGADO", "VENCIDO", "PENDIENTE", "EN_REVISION", "ANULADO"};
            List<Compromiso> datos = new ArrayList<>();
            LocalDate hoy = LocalDate.now();
            for (int tipo = 0; tipo < etiquetas.length; tipo++) {
                for (int numero = 1; numero <= 5; numero++) {
                    Compromiso c = new Compromiso();
                    c.setIdUsuario(idUsuario);
                    c.setIdEmpresa(empresas.get(nombresEmpresas[tipo]));
                    c.setNombre("DEMO - " + etiquetas[tipo] + " " + numero);
                    c.setMonto(BigDecimal.valueOf(10000 + (tipo + 1) * 15000 + numero * 2250L));
                    c.setFechaVencimiento(switch (tipo) {
                        case 0 -> hoy.minusDays(30L + numero * 5L);
                        case 1 -> hoy.minusDays(2L + numero * 3L);
                        case 2 -> hoy.plusDays(5L + numero * 7L);
                        case 3 -> hoy.plusDays(12L + numero * 4L);
                        default -> hoy.plusDays(18L + numero * 3L);
                    });
                    c.setEstado(tipo == 1 ? "PENDIENTE" : etiquetas[tipo]);
                    c.setActivo(true);
                    datos.add(c);
                }
            }
            compromisos.saveAll(datos);
            inicializado = true;
            log.info("Datos iniciales: 25 compromisos de demostracion creados");
        } catch (Exception ex) {
            log.warn("Datos demo de compromisos pendientes; se reintentara: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> obtener(String url, boolean conAdministrador) {
        RestClient.RequestHeadersSpec<?> peticion = http.get().uri(url);
        if (conAdministrador) {
            peticion = peticion.headers(headers -> headers.setBasicAuth(ADMIN, CLAVE));
        }
        List<?> elementos = peticion.retrieve().body(List.class);
        if (elementos == null) return List.of();
        return (List<Map<String, Object>>) (List<?>) elementos;
    }

    private Map<String, Object> encontrarPorTexto(List<Map<String, Object>> datos, String campo, String valor) {
        for (Map<String, Object> dato : datos) {
            if (valor.equals(dato.get(campo))) return dato;
        }
        return null;
    }

    private long numero(Map<String, Object> datos, String campo) {
        return ((Number) datos.get(campo)).longValue();
    }
}
