package cl.pagoflex.archivos.controller;

import cl.pagoflex.archivos.controller.ArchivoController;
import cl.pagoflex.archivos.service.ArchivoService;
import cl.pagoflex.archivos.model.Archivo;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.springframework.core.io.ByteArrayResource;


class ControladorEndpointsTest {
    @Test void todosLosEndpointsDeleganSinLevantarServidor() throws Exception {
        ArchivoService mockService = mock(ArchivoService.class);
        Archivo archivo = new Archivo();
        archivo.setNombreOriginal("recibo.pdf");
        archivo.setTipoMime("application/pdf");
        archivo.setTamanoBytes(5L);
        when(mockService.buscarPorId(anyLong())).thenReturn(archivo);
        when(mockService.descargar(anyLong())).thenReturn(new ByteArrayResource(new byte[]{1,2,3,4,5}));
        ArchivoController controlador = new ArchivoController(mockService);
        int encontrados = 0;
        for (Method endpoint : ArchivoController.class.getDeclaredMethods()) {
            if (!ResponseEntity.class.isAssignableFrom(endpoint.getReturnType())) continue;
            Object[] argumentos = Arrays.stream(endpoint.getParameterTypes())
                .map(this::argumentoDePrueba).toArray();
            ResponseEntity<?> respuesta;
            try {
                respuesta = (ResponseEntity<?>) endpoint.invoke(controlador,argumentos);
            } catch (InvocationTargetException e) {
                throw new AssertionError("Fallo en " + endpoint.getName(),e.getCause());
            }
            assertTrue(respuesta.getStatusCode().is2xxSuccessful(),endpoint.getName());
            encontrados++;
        }
        assertTrue(encontrados >= 4);
        assertTrue(mockingDetails(mockService).getInvocations().size() >= encontrados);
    }
    private Object argumentoDePrueba(Class<?> tipo) {
        if (tipo == String.class) return "Basic YTpi";
        if (tipo == Long.class || tipo == long.class) return 7L;
        if (tipo.getName().equals("org.springframework.web.multipart.MultipartFile"))
            return mock(org.springframework.web.multipart.MultipartFile.class);
        try {
            if (tipo.isRecord()) {
                var componentes=tipo.getRecordComponents();
                Class<?>[] tipos=Arrays.stream(componentes).map(c -> c.getType()).toArray(Class<?>[]::new);
                Object[] valores=Arrays.stream(tipos).map(this::valorSimple).toArray();
                return tipo.getDeclaredConstructor(tipos).newInstance(valores);
            }
            return tipo.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Tipo no contemplado: " + tipo,e);
        }
    }
    private Object valorSimple(Class<?> tipo) {
        if (tipo == String.class) return "Valor";
        if (tipo == Long.class) return 7L;
        if (tipo == Boolean.class) return true;
        throw new IllegalArgumentException("Tipo record no contemplado: " + tipo);
    }
}
