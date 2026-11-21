package ar.edu.et35.panol;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import ar.edu.et35.panol.Modelo.Empleado;
import ar.edu.et35.panol.Modelo.Prestamo;

/** Historial del empleado: trabajos realizados y en qué estado devolvió cada tablet. */
public class HistorialEmpleadoActivity extends BaseActivity {
    @Override protected String titulo() { return "Historial del empleado"; }

    @Override protected void construir() {
        Empleado e = db.empleado(getIntent().getIntExtra("legajo", 0));
        if (e == null) {
            content.addView(Ui.tv(this, "Empleado no encontrado.", 16, false, Ui.TEXTO));
            return;
        }
        List<Prestamo> ps = db.prestamos(e.legajo);
        int buenos = 0, danos = 0, enPoder = 0;
        for (Prestamo p : ps) {
            if (!p.devuelta()) enPoder++;
            else if ("CON_DANOS".equals(p.estadoFisico)) danos++;
            else buenos++;
        }

        LinearLayout cab = Ui.tarjeta(this);
        cab.addView(Ui.tv(this, e.legajo + " · " + e.nombre, 20, true, Ui.TITULO));
        cab.addView(Ui.tv(this, e.area == null || e.area.isEmpty() ? "Sin área" : e.area, 14, false, Ui.TEXTO2));
        LinearLayout.LayoutParams lp = Ui.lp(this, 8);
        lp.width = -2;
        cab.addView(e.activo ? Ui.chip(this, "Activo", 0xFF2E7D32) : Ui.chip(this, "Desactivado", 0xFF6D4C41), lp);
        cab.addView(Ui.fila(this, "Retiros", String.valueOf(ps.size())), Ui.lp(this, 10));
        cab.addView(Ui.fila(this, "Devueltas en buen estado", String.valueOf(buenos)));
        cab.addView(Ui.fila(this, "Devueltas con daños", String.valueOf(danos)));
        cab.addView(Ui.fila(this, "Aún en su poder", String.valueOf(enPoder)));
        content.addView(cab);

        content.addView(Ui.tv(this, "Trabajos realizados", 15, true, Ui.TITULO), Ui.lp(this, 18));
        if (ps.isEmpty()) {
            content.addView(Ui.tv(this, "Todavía no retiró ninguna tablet.", 14, false, Ui.TEXTO2), Ui.lp(this, 8));
        }
        for (Prestamo p : ps) {
            LinearLayout c = Ui.tarjeta(this);
            c.addView(Ui.tv(this, p.serie, 18, true, Ui.TITULO));
            TextView chip;
            if (!p.devuelta()) chip = Ui.chip(this, "Aún en su poder", 0xFF1565C0);
            else if ("CON_DANOS".equals(p.estadoFisico)) chip = Ui.chip(this, "Devuelta con daños", 0xFFC62828);
            else chip = Ui.chip(this, "Devuelta en buen estado", 0xFF2E7D32);
            LinearLayout.LayoutParams l2 = Ui.lp(this, 6);
            l2.width = -2;
            c.addView(chip, l2);
            c.addView(Ui.fila(this, "Trabajo", p.tarea != null ? p.tarea : "Sin tarea asociada"), Ui.lp(this, 8));
            c.addView(Ui.fila(this, "Tipo de uso", "USO_EXTERNO".equals(p.uso) ? "Home Office" : "Local (oficina)"));
            c.addView(Ui.fila(this, "Retiro", p.fechaRetiro));
            c.addView(Ui.fila(this, "Devolución", p.devuelta() ? p.fechaDevolucion : "Pendiente"));
            if (p.devuelta()) {
                String est = "CON_DANOS".equals(p.estadoFisico) ? "Con daños" : "Buen estado";
                c.addView(Ui.fila(this, "Estado al devolver", est));
                if (p.observaciones != null && !p.observaciones.isEmpty())
                    c.addView(Ui.fila(this, "Observaciones", p.observaciones));
            }
            content.addView(c, Ui.lp(this, 10));
        }
    }
}
