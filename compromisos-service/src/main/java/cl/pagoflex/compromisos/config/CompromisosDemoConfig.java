package cl.pagoflex.compromisos.config;

import cl.pagoflex.compromisos.model.Compromiso;
import cl.pagoflex.compromisos.repository.CompromisoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
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
    private static final String CLAVE = "PagoFlex123";

    private static final String[] EMPRESAS = {
            "Aguas Cordillera", "Energía Metropolitana", "Conecta Hogar Fibra",
            "Comercial Los Robles", "Servicios Residenciales Los Aromos"
    };

    private static final String[] ESTADOS = {
            "PAGADO", "VENCIDO", "PENDIENTE", "EN_REVISION", "ANULADO"
    };

    private static final String[][] NOMBRES = {
            {
                    "Boleta de agua potable - domicilio",
                    "Consumo de agua - departamento",
                    "Cuenta de agua - segunda vivienda",
                    "Boleta de alcantarillado",
                    "Servicio de agua - local comercial"
            },
            {
                    "Cuenta de electricidad - hogar",
                    "Factura de luz - departamento",
                    "Consumo eléctrico - oficina",
                    "Boleta de electricidad - bodega",
                    "Suministro eléctrico - local"
            },
            {
                    "Internet fibra 600 Mbps",
                    "Plan de internet hogar",
                    "Fibra óptica - oficina",
                    "Internet y televisión familiar",
                    "Servicio de conectividad - domicilio"
            },
            {
                    "Compra mensual de supermercado",
                    "Compra de artículos de limpieza",
                    "Pedido de productos del hogar",
                    "Compra de mercadería",
                    "Abastecimiento de despensa"
            },
            {
                    "Gastos comunes - departamento",
                    "Cuota de mantención del edificio",
                    "Servicio de aseo mensual",
                    "Arriendo de estacionamiento",
                    "Mantención de áreas comunes"
            }
    };

    private final CompromisoRepository compromisos;
    private final RestClient http = RestClient.create();
    private boolean inicializado;

    public CompromisosDemoConfig(CompromisoRepository compromisos) {
        this.compromisos = compromisos;
    }

    @Scheduled(initialDelay = 9000, fixedDelay = 20000)
    public synchronized void cargarDatosDeDemostracion() {
        if (inicializado) return;
        try {
            if (compromisos.count() > 0) {
                actualizarTitulosIniciales();
                inicializado = true;
                return;
            }

            Map<String, Object> usuario = encontrarPorTexto(
                    obtener("http://localhost:8081/api/v1/usuarios", true),
                    "correo", "crojas@correo.ejemplo");
            if (usuario == null) return;
            long idUsuario = numero(usuario, "idUsuario");

            Map<String, Long> empresas = new HashMap<>();
            for (Map<String, Object> empresa : obtener("http://localhost:8082/api/v1/empresas", false)) {
                empresas.put(String.valueOf(empresa.get("nombre")), numero(empresa, "idEmpresa"));
            }
            for (String nombre : EMPRESAS) {
                if (!empresas.containsKey(nombre)) return;
            }

            List<Compromiso> datos = new ArrayList<>();
            LocalDate hoy = LocalDate.now();
            for (int tipo = 0; tipo < ESTADOS.length; tipo++) {
                for (int numero = 1; numero <= 5; numero++) {
                    Compromiso c = new Compromiso();
                    c.setIdUsuario(idUsuario);
                    c.setIdEmpresa(empresas.get(EMPRESAS[tipo]));
                    c.setNombre(NOMBRES[tipo][numero - 1]);
                    c.setMonto(BigDecimal.valueOf(10000L + (tipo + 1) * 15000L + numero * 2250L));
                    c.setFechaVencimiento(switch (tipo) {
                        case 0 -> hoy.minusDays(30L + numero * 5L);
                        case 1 -> hoy.minusDays(2L + numero * 3L);
                        case 2 -> hoy.plusDays(5L + numero * 7L);
                        case 3 -> hoy.plusDays(12L + numero * 4L);
                        default -> hoy.plusDays(18L + numero * 3L);
                    });
                    c.setEstado(tipo == 1 ? "PENDIENTE" : ESTADOS[tipo]);
                    c.setActivo(true);
                    datos.add(c);
                }
            }
            compromisos.saveAll(datos);
            inicializado = true;
            log.info("Veinticinco compromisos iniciales creados");
        } catch (Exception ex) {
            log.warn("Inicialización de compromisos pendiente: {}", ex.getMessage());
        }
    }

    private void actualizarTitulosIniciales() {
        for (Compromiso compromiso : compromisos.findAll()) {
            String nombre = compromiso.getNombre();
            if (nombre == null) continue;
            for (int tipo = 0; tipo < ESTADOS.length; tipo++) {
                for (int numero = 1; numero <= 5; numero++) {
                    if (nombre.equals("DEMO - " + ESTADOS[tipo] + " " + numero)) {
                        compromiso.setNombre(NOMBRES[tipo][numero - 1]);
                        compromisos.save(compromiso);
                    }
                }
            }
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
