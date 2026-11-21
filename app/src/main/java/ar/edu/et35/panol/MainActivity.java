package ar.edu.et35.panol;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

public class MainActivity extends BaseActivity {
    private LinearLayout resumen;

    @Override protected String titulo() { return "Pañol-App · Escuela Técnica N° 35"; }

    @Override protected void construir() {
        resumen = Ui.tarjeta(this);
        content.addView(resumen);
        menu("Escanear QR / Ficha técnica", Ui.PRIMARIO, FichaActivity.class);
        menu("Despachar tablet (préstamo)", Ui.SECUNDARIO, DespachoActivity.class);
        menu("Recibir devolución", 0xFF2E7D32, DevolucionActivity.class);
        menu("Alta de tablet + generar QR", 0xFF6A4C93, AltaActivity.class);
        menu("Inventario y estados", Ui.BTN_GRIS, InventarioActivity.class);
        menu("Empleados y tareas", Ui.BTN_GRIS, EmpleadosActivity.class);
        menu("🔄  Compartir datos con otros celulares", Ui.BTN_GRIS, CompartirActivity.class);
        content.addView(Ui.boton(this, "🎨  Color de la app", Ui.BTN_GRIS, (View v) -> elegirColor()), Ui.lp(this, 12));
    }

    private void elegirColor() {
        dialogo().setTitle("Color de la app")
                .setSingleChoiceItems(Tema.NOMBRES, Tema.actual, (d, which) -> {
                    Tema.guardar(this, which);
                    d.dismiss();
                    recreate();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void menu(String t, int color, Class<?> c) {
        content.addView(Ui.boton(this, t, color, (View v) -> startActivity(new Intent(this, c))), Ui.lp(this, 12));
    }

    @Override protected void onResume() {
        super.onResume();
        resumen.removeAllViews();
        resumen.addView(Ui.tv(this, "Estado del parque de tablets", 15, true, Ui.TITULO));
        for (Estado e : Estado.values()) {
            LinearLayout f = Ui.fila(this, e.etiqueta, String.valueOf(db.contar(e)));
            resumen.addView(f);
        }
    }
}
