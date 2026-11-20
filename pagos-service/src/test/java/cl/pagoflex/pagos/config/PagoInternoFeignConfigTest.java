package cl.pagoflex.pagos.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


class PagoInternoFeignConfigTest {
    @Test void soloLasPeticionesInternasDeConfirmacionLlevanElSecreto() {
        var interceptor = new PagoInternoFeignConfig().autenticacionInternaFeign("s".repeat(40));
        RequestTemplate interna=new RequestTemplate();
        interna.method("POST");
        interna.uri("/internal/compromisos/32/marcar-pagado");
        interceptor.apply(interna);
        assertTrue(interna.headers().containsKey("X-PagoFlex-Internal-Secret"));
        RequestTemplate publica=new RequestTemplate();
        publica.method("GET");
        publica.uri("/api/v1/compromisos/32");
        interceptor.apply(publica);
        assertFalse(publica.headers().containsKey("X-PagoFlex-Internal-Secret"));
    }
    @Test void noEnviaClaveDemasiadoCorta() {
        var interceptor = new PagoInternoFeignConfig().autenticacionInternaFeign("corta");
        RequestTemplate r=new RequestTemplate();
        r.method("POST");
        r.uri("/internal/compromisos/32/marcar-pagado");
        interceptor.apply(r);
        assertFalse(r.headers().containsKey("X-PagoFlex-Internal-Secret"));
    }
}
