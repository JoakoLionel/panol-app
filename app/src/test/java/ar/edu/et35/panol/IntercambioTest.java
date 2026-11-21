package ar.edu.et35.panol;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class IntercambioTest {
    private Intercambio.Snapshot ejemplo() {
        Intercambio.Snapshot s = new Intercambio.Snapshot();
        Intercambio.Emp e = new Intercambio.Emp(); e.legajo = 1001; e.nombre = "Rubén"; e.area = "Sistemas"; s.empleados.add(e);
        Intercambio.Tar t = new Intercambio.Tar(); t.uid = "u1"; t.legajo = 1001; t.descripcion = "Informe"; t.homeOffice = true; t.activa = true; t.fecha = "2026-11-20 10:00"; s.tareas.add(t);
        Intercambio.Tab b = new Intercambio.Tab(); b.serie = "TAB-1"; b.procesador = "cpu"; b.fechaAlta = "2026-11-20 09:00"; s.tablets.add(b);
        s.movimientos.add(mov("m1", "ALTA", null, "EN_PANOL", 0, null, "2026-11-20 09:00"));
        s.movimientos.add(mov("m2", "DESPACHO", "EN_PANOL", "USO_EXTERNO", 1001, "u1", "2026-11-20 10:05"));
        return s;
    }
    private Intercambio.Mov mov(String uid, String tipo, String o, String d, int leg, String tar, String f) {
        Intercambio.Mov m = new Intercambio.Mov(); m.uid = uid; m.serie = "TAB-1"; m.tipo = tipo; m.origen = o; m.destino = d;
        m.legajo = leg; m.tareaUid = tar; m.fecha = f; return m;
    }

    @Test public void roundtrip() {
        Intercambio.Snapshot r = Intercambio.parse(Intercambio.toJson(ejemplo(), "2026-11-20 12:00"));
        assertEquals(1, r.empleados.size()); assertEquals("Rubén", r.empleados.get(0).nombre);
        assertEquals(1, r.tareas.size()); assertTrue(r.tareas.get(0).homeOffice);
        assertEquals("TAB-1", r.tablets.get(0).serie);
        assertEquals(2, r.movimientos.size());
        assertEquals("u1", r.movimientos.get(1).tareaUid);
        assertEquals(1001, r.movimientos.get(1).legajo);
        assertNull(r.movimientos.get(0).tareaUid);
    }
    @Test(expected = IllegalArgumentException.class) public void noEsJson() { Intercambio.parse("hola"); }
    @Test(expected = IllegalArgumentException.class) public void otraApp() { Intercambio.parse("{\"app\":\"otra\",\"version\":1}"); }
    @Test(expected = IllegalArgumentException.class) public void versionFutura() { Intercambio.parse("{\"app\":\"panol-app\",\"version\":99}"); }
    @Test(expected = IllegalArgumentException.class) public void faltanSecciones() { Intercambio.parse("{\"app\":\"panol-app\",\"version\":1}"); }
    @Test(expected = IllegalArgumentException.class) public void estadoInvalido() {
        String j = Intercambio.toJson(ejemplo(), "x").replace("USO_EXTERNO", "VOLANDO"); Intercambio.parse(j);
    }
    @Test(expected = IllegalArgumentException.class) public void tipoInvalido() {
        String j = Intercambio.toJson(ejemplo(), "x").replace("DESPACHO", "ROBO"); Intercambio.parse(j);
    }
    @Test(expected = IllegalArgumentException.class) public void legajoInvalido() {
        String j = Intercambio.toJson(ejemplo(), "x").replace("\"legajo\":1001,\"nombre\"", "\"legajo\":-5,\"nombre\""); Intercambio.parse(j);
    }

    @Test public void estadoDesdeHistorial() {
        List<Intercambio.Mov> l = new ArrayList<>();
        assertEquals(Estado.EN_PANOL, Intercambio.ultimoEstado(l).estado);
        l.add(mov("a", "ALTA", null, "EN_PANOL", 0, null, "2026-11-20 09:00"));
        l.add(mov("b", "DESPACHO", "EN_PANOL", "USO_LOCAL", 1002, null, "2026-11-20 10:00"));
        Intercambio.Ultimo u = Intercambio.ultimoEstado(l);
        assertEquals(Estado.USO_LOCAL, u.estado); assertEquals(1002, u.legajo);
        l.add(mov("c", "DEVOLUCION", "USO_LOCAL", "SERVICIO_TECNICO", 1002, null, "2026-11-20 17:00"));
        u = Intercambio.ultimoEstado(l);
        assertEquals(Estado.SERVICIO_TECNICO, u.estado); assertEquals(0, u.legajo);
    }
    @Test public void ganaElMasRecienteSinImportarElOrden() {
        // el celular B importó primero un movimiento nuevo y después uno viejo: manda la fecha, no el orden
        List<Intercambio.Mov> l = new ArrayList<>();
        l.add(mov("nuevo", "DEVOLUCION", "USO_EXTERNO", "EN_PANOL", 1001, null, "2026-11-21 08:00"));
        l.add(mov("viejo", "DESPACHO", "EN_PANOL", "USO_EXTERNO", 1001, null, "2026-11-20 10:00"));
        Intercambio.Ultimo u = Intercambio.ultimoEstado(l);
        assertEquals(Estado.EN_PANOL, u.estado); assertEquals(0, u.legajo);
    }
}
