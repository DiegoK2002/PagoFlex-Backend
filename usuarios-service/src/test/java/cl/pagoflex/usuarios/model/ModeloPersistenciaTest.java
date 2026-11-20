package cl.pagoflex.usuarios.model;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;


class ModeloPersistenciaTest {
    @Test
    void propiedadesDeUsuarioConservanSusValores() throws Exception {
        verificarPropiedades(Usuario.class);
    }

    @Test
    void propiedadesDeRolConservanSusValores() throws Exception {
        verificarPropiedades(Rol.class);
    }

    private void verificarPropiedades(Class<?> tipo) throws Exception {
        Object instancia = tipo.getDeclaredConstructor().newInstance();
        int propiedades = 0;
        for (Method setter : tipo.getMethods()) {
            if (!setter.getName().startsWith("set") || setter.getParameterCount() != 1) {
                continue;
            }
            String nombre = setter.getName().substring(3);
            Method getter = tipo.getMethod("get" + nombre);
            Object valor = valorDePrueba(setter.getParameterTypes()[0]);
            setter.invoke(instancia, valor);
            assertEquals(valor, getter.invoke(instancia), nombre);
            propiedades++;
        }
        assertTrue(propiedades >= 2);
    }

    private Object valorDePrueba(Class<?> tipo) throws Exception {
        if (tipo == String.class) return "Valor de prueba";
        if (tipo == Long.class || tipo == long.class) return 42L;
        if (tipo == Integer.class || tipo == int.class) return 42;
        if (tipo == Boolean.class || tipo == boolean.class) return Boolean.FALSE;
        if (tipo == BigDecimal.class) return new BigDecimal("12345.67");
        if (tipo == LocalDate.class) return LocalDate.of(2026, 11, 20);
        if (tipo == LocalDateTime.class) return LocalDateTime.of(2026, 11, 20, 12, 0);
        if (tipo.getSimpleName().equals("Rol")) return tipo.getDeclaredConstructor(Long.class, String.class).newInstance(3L, "ADMINISTRADOR");
        throw new IllegalArgumentException("Tipo de propiedad no contemplado: " + tipo);
    }
}
