package cl.pagoflex.pagos.config;

import cl.pagoflex.pagos.model.Pago;
import cl.pagoflex.pagos.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableScheduling
public class PagosDemoConfig {

    private static final Logger log = LoggerFactory.getLogger(PagosDemoConfig.class);
    private static final Set<String> COMPROMISOS_PAGADOS = Set.of(
            "Boleta de agua potable - domicilio",
            "Consumo de agua - departamento",
            "Cuenta de agua - segunda vivienda",
            "Boleta de alcantarillado",
            "Servicio de agua - local comercial"
    );

    private final PagoRepository pagos;
    private final RestClient http = RestClient.create();
    private boolean inicializado;

    public PagosDemoConfig(PagoRepository pagos) {
        this.pagos = pagos;
    }

    @Scheduled(initialDelay = 13000, fixedDelay = 20000)
    public synchronized void cargarDatosDeDemostracion() {
        if (inicializado) return;
        try {
            if (pagos.count() > 0) {
                actualizarReferenciasIniciales();
                inicializado = true;
                return;
            }

            List<Map<String, Object>> compromisos = obtenerCompromisos();
            List<Pago> datos = new ArrayList<>();
            for (Map<String, Object> compromiso : compromisos) {
                String nombre = String.valueOf(compromiso.get("nombre"));
                if (!COMPROMISOS_PAGADOS.contains(nombre)
                        && !nombre.startsWith("DEMO - PAGADO ")) continue;
                if (!"PAGADO".equals(String.valueOf(compromiso.get("estado")))) continue;

                long idCompromiso = numero(compromiso, "idCompromiso");
                Pago pago = new Pago();
                pago.setIdCompromiso(idCompromiso);
                pago.setIdUsuario(numero(compromiso, "idUsuario"));
                pago.setMonto(new BigDecimal(String.valueOf(compromiso.get("monto"))));
                pago.setFechaPago(LocalDate.parse(String.valueOf(compromiso.get("fechaVencimiento"))).minusDays(2));
                pago.setMetodoPago("TRANSFERENCIA");
                pago.setReferenciaComprobante("TRF-PF-" + idCompromiso);
                pago.setActivo(true);
                pago.setEstadoSincronizacion("CONFIRMADO");
                datos.add(pago);
            }
            if (datos.size() != 5) return;
            pagos.saveAll(datos);
            inicializado = true;
            log.info("Cinco pagos iniciales confirmados y puntuales creados");
        } catch (Exception ex) {
            log.warn("Inicialización de pagos pendiente: {}", ex.getMessage());
        }
    }

    private void actualizarReferenciasIniciales() {
        for (Pago pago : pagos.findAll()) {
            String referencia = pago.getReferenciaComprobante();
            if (referencia != null && referencia.startsWith("DEMO-COMPROBANTE-")) {
                pago.setReferenciaComprobante("TRF-PF-" + pago.getIdCompromiso());
                pagos.save(pago);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> obtenerCompromisos() {
        List<?> valores = http.get()
                .uri("http://localhost:8083/api/v1/compromisos")
                .headers(h -> h.setBasicAuth("admin@correo.ejemplo", "PagoFlex123"))
                .retrieve()
                .body(List.class);
        if (valores == null) return List.of();
        return (List<Map<String, Object>>) (List<?>) valores;
    }

    private long numero(Map<String, Object> datos, String campo) {
        return ((Number) datos.get(campo)).longValue();
    }
}
