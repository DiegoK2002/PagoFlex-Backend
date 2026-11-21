package cl.pagoflex.empresas.config;

import cl.pagoflex.empresas.model.Empresa;
import cl.pagoflex.empresas.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;

@Configuration
@EnableScheduling
public class EmpresasDemoConfig {

    private static final Logger log = LoggerFactory.getLogger(EmpresasDemoConfig.class);
    private static final Map<String, String> NOMBRES_ANTIGUOS = Map.of(
            "Agua Cordillera Demo", "Aguas Cordillera",
            "Energia Central Demo", "Energía Metropolitana",
            "Fibra Ciudad Demo", "Conecta Hogar Fibra",
            "Comercio Vecinal Demo", "Comercial Los Robles",
            "Servicios Urbanos Demo", "Servicios Residenciales Los Aromos"
    );

    private final EmpresaRepository empresas;
    private boolean inicializado;

    public EmpresasDemoConfig(EmpresaRepository empresas) {
        this.empresas = empresas;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 20000)
    public synchronized void cargarDatosDeDemostracion() {
        if (inicializado) return;
        try {
            if (empresas.count() == 0) {
                empresas.saveAll(List.of(
                        empresa("Aguas Cordillera", "Agua"),
                        empresa("Energía Metropolitana", "Electricidad"),
                        empresa("Conecta Hogar Fibra", "Telecomunicaciones"),
                        empresa("Comercial Los Robles", "Comercio"),
                        empresa("Servicios Residenciales Los Aromos", "Servicios")
                ));
                log.info("Cinco empresas iniciales creadas");
            } else {
                for (Empresa empresa : empresas.findAll()) {
                    String nuevoNombre = NOMBRES_ANTIGUOS.get(empresa.getNombre());
                    if (nuevoNombre != null) {
                        empresa.setNombre(nuevoNombre);
                        empresas.save(empresa);
                    }
                }
            }
            inicializado = true;
        } catch (Exception ex) {
            log.warn("Inicialización de empresas pendiente: {}", ex.getMessage());
        }
    }

    private Empresa empresa(String nombre, String categoria) {
        Empresa empresa = new Empresa();
        empresa.setNombre(nombre);
        empresa.setCategoria(categoria);
        empresa.setActivo(true);
        return empresa;
    }
}
