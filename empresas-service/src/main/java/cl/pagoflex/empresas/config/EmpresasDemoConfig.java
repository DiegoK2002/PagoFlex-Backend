package cl.pagoflex.empresas.config;

import cl.pagoflex.empresas.model.Empresa;
import cl.pagoflex.empresas.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
public class EmpresasDemoConfig {

    private static final Logger log = LoggerFactory.getLogger(EmpresasDemoConfig.class);
    private final EmpresaRepository empresas;
    private boolean inicializado;

    public EmpresasDemoConfig(EmpresaRepository empresas) {
        this.empresas = empresas;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 20000)
    public synchronized void cargarDatosDeDemostracion() {
        if (inicializado) return;
        if (empresas.count() > 0) {
            inicializado = true;
            return;
        }
        try {
            empresas.saveAll(List.of(
                    empresa("Agua Cordillera Demo", "Agua"),
                    empresa("Energia Central Demo", "Electricidad"),
                    empresa("Fibra Ciudad Demo", "Telecomunicaciones"),
                    empresa("Comercio Vecinal Demo", "Comercio"),
                    empresa("Servicios Urbanos Demo", "Servicios")
            ));
            inicializado = true;
            log.info("Datos iniciales: cinco empresas de demostracion creadas");
        } catch (Exception ex) {
            log.warn("No se pudieron crear empresas demo; se reintentara: {}", ex.getMessage());
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
