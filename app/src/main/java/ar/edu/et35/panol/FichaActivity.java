package ar.edu.et35.panol;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;

import ar.edu.et35.panol.Modelo.Movimiento;

/** Sprint 1: escanear el QR y mostrar toda la ficha técnica. */
public class FichaActivity extends BaseActivity {
    private LinearLayout resultado;

    @Override protected String titulo() { return "Ficha técnica (escaneo QR)"; }

    @Override protected void construir() {
        content.addView(Ui.boton(this, "📷  Escanear código QR", Ui.PRIMARIO, v -> escanear(this::mostrar)));
        content.addView(Ui.boton(this, "⌨  QR dañado: ingresar serie a mano", Ui.BTN_GRIS, v -> ingresoManual(this::mostrar)),
                Ui.lp(this, 10));
        resultado = new LinearLayout(this);
        resultado.setOrientation(LinearLayout.VERTICAL);
        content.addView(resultado, Ui.lp(this, 16));

        String serie = getIntent().getStringExtra("serie");
        if (serie != null) {
            Tablet t = db.tablet(serie);
            if (t != null) mostrar(t, true);
        }
    }

    private void mostrar(Tablet t, boolean registrada) {
        resultado.removeAllViews();
        resultado.addView(fichaTablet(t, registrada));
        if (!registrada) {
            resultado.addView(Ui.boton(this, "Registrar esta tablet en el pañol", 0xFF6A4C93, (View v) -> {
                Intent i = new Intent(this, AltaActivity.class);
                i.putExtra("raw", QrCodec.toJson(t));
                startActivity(i);
            }), Ui.lp(this, 12));
            return;
        }
        List<Movimiento> hist = db.historial(t.serie, 8);
        LinearLayout h = Ui.tarjeta(this);
        h.addView(Ui.tv(this, "Últimos movimientos", 15, true, Ui.TITULO));
        if (hist.isEmpty()) h.addView(Ui.tv(this, "Sin movimientos.", 14, false, Ui.TEXTO2));
        for (Movimiento m : hist) {
            String l = m.fecha + " · " + m.tipo
                    + (m.empleado != null ? " · " + m.empleado : "")
                    + (m.tarea != null ? "\n   Tarea: " + m.tarea : "")
                    + (m.estadoFisico != null ? "\n   Estado físico: " + m.estadoFisico : "")
                    + (m.observaciones != null && !m.observaciones.isEmpty() ? "\n   " + m.observaciones : "");
            h.addView(Ui.tv(this, l, 13, false, Ui.TEXTO), Ui.lp(this, 8));
        }
        resultado.addView(h, Ui.lp(this, 12));
    }
}
