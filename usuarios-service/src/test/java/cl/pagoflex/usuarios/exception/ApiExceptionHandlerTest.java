package cl.pagoflex.usuarios.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ApiExceptionHandlerTest {
    @Test void errorDeDuplicadoDevuelve409YCodigo() {
        var resp=new ApiExceptionHandler().usuarioDuplicado(new UsuarioDuplicadoException("Correo repetido"));
        assertEquals(HttpStatus.CONFLICT,resp.getStatusCode());
        assertEquals("USUARIO_DUPLICADO",resp.getBody().get("codigo"));
        assertEquals("Correo repetido",resp.getBody().get("mensaje"));
    }
    @Test void argumentoInvalidoDevuelve400YCodigo() {
        var resp=new ApiExceptionHandler().datosInvalidos(new IllegalArgumentException("Dato invalido"));
        assertEquals(HttpStatus.BAD_REQUEST,resp.getStatusCode());
        assertEquals("DATOS_INVALIDOS",resp.getBody().get("codigo"));
    }
    @Test void validaErroresDeCamposSinDuplicarlos() {
        var ex=mock(MethodArgumentNotValidException.class);
        var resultado=new BeanPropertyBindingResult(new Object(),"datos");
        resultado.addError(new FieldError("datos","correo","Invalido"));
        resultado.addError(new FieldError("datos","correo","Duplicado"));
        when(ex.getBindingResult()).thenReturn(resultado);
        var response=new ApiExceptionHandler().validarCampos(ex);
        assertEquals(HttpStatus.BAD_REQUEST,response.getStatusCode());
        assertEquals("VALIDACION",response.getBody().get("codigo"));
        @SuppressWarnings("unchecked")
        var campos=(java.util.Map<String,String>)response.getBody().get("campos");
        assertEquals("Invalido",campos.get("correo"));
        assertEquals(1,campos.size());
    }
}
