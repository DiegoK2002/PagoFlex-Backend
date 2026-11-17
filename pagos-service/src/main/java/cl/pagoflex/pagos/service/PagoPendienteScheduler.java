package cl.pagoflex.pagos.service;

import cl.pagoflex.pagos.model.Pago;
import cl.pagoflex.pagos.repository.PagoRepository;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class PagoPendienteScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(PagoPendienteScheduler.class);

    private final PagoRepository pagoRepository;
    private final PagoService pagoService;

    public PagoPendienteScheduler(
            PagoRepository pagoRepository,
            PagoService pagoService
    ) {
        this.pagoRepository = pagoRepository;
        this.pagoService = pagoService;
    }

    @Scheduled(
            initialDelay = 15000,
            fixedDelay = 60000
    )
    public void recuperarPagosPendientes() {

        List<Pago> pendientes =
                pagoRepository
                        .findByEstadoSincronizacionAndActivoTrue(
                                "PENDIENTE"
                        );

        for (Pago pago : pendientes) {
            try {
                pagoService.confirmarPendiente(
                        pago.getIdPago()
                );

                logger.info(
                        "Pago {} sincronizado correctamente",
                        pago.getIdPago()
                );

            } catch (Exception error) {
                logger.warn(
                        "No se pudo sincronizar el pago {}: {}",
                        pago.getIdPago(),
                        error.getMessage()
                );
            }
        }
    }
}
