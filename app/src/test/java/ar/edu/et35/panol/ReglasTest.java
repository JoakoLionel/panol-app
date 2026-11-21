package ar.edu.et35.panol;

import static org.junit.Assert.*;
import org.junit.Test;

public class ReglasTest {
    @Test public void despachoLocalOk() {
        assertNull(Reglas.validarDespacho(Estado.EN_PANOL, Reglas.Uso.LOCAL, true, false));
        assertEquals(Estado.USO_LOCAL, Reglas.destinoDespacho(Reglas.Uso.LOCAL));
    }
    @Test public void homeOfficeSinTareaSeRechaza() {
        String m = Reglas.validarDespacho(Estado.EN_PANOL, Reglas.Uso.EXTERNO, true, false);
        assertNotNull(m);
        assertTrue(m.startsWith("ALERTA"));
    }
    @Test public void homeOfficeConTareaOk() {
        assertNull(Reglas.validarDespacho(Estado.EN_PANOL, Reglas.Uso.EXTERNO, true, true));
        assertEquals(Estado.USO_EXTERNO, Reglas.destinoDespacho(Reglas.Uso.EXTERNO));
    }
    @Test public void soloSeDespachaDesdePanol() {
        for (Estado e : new Estado[]{Estado.USO_LOCAL, Estado.USO_EXTERNO, Estado.SERVICIO_TECNICO})
            assertNotNull(Reglas.validarDespacho(e, Reglas.Uso.LOCAL, true, true));
    }
    @Test public void requiereEmpleado() {
        assertNotNull(Reglas.validarDespacho(Estado.EN_PANOL, Reglas.Uso.LOCAL, false, false));
    }
    @Test public void devolucion() {
        assertNull(Reglas.validarDevolucion(Estado.USO_LOCAL));
        assertNull(Reglas.validarDevolucion(Estado.USO_EXTERNO));
        assertNotNull(Reglas.validarDevolucion(Estado.EN_PANOL));
        assertNotNull(Reglas.validarDevolucion(Estado.SERVICIO_TECNICO));
        assertEquals(Estado.EN_PANOL, Reglas.destinoDevolucion(false));
        assertEquals(Estado.SERVICIO_TECNICO, Reglas.destinoDevolucion(true));
    }
    @Test public void finServicio() {
        assertNull(Reglas.validarFinServicio(Estado.SERVICIO_TECNICO));
        assertNotNull(Reglas.validarFinServicio(Estado.EN_PANOL));
    }
    @Test public void eliminarEmpleado() {
        assertEquals(Reglas.AccionEliminar.BLOQUEADO, Reglas.accionEliminarEmpleado(true, 0));
        assertEquals(Reglas.AccionEliminar.BLOQUEADO, Reglas.accionEliminarEmpleado(true, 5));
        assertEquals(Reglas.AccionEliminar.BORRAR, Reglas.accionEliminarEmpleado(false, 0));
        assertEquals(Reglas.AccionEliminar.DESACTIVAR, Reglas.accionEliminarEmpleado(false, 3));
    }
}
