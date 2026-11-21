package ar.edu.et35.panol;

import android.content.Intent;
import android.widget.LinearLayout;

import java.util.List;

public class InventarioActivity extends BaseActivity {
    @Override protected String titulo() { return "Inventario y estados"; }

    @Override protected void construir() {}

    @Override protected void onResume() {
        super.onResume();
        content.removeAllViews();
        List<Tablet> ts = db.tablets();
        if (ts.isEmpty()) {
            content.addView(Ui.tv(this, "Todavía no hay tablets registradas. Usá \"Alta de tablet\".", 15, false, Ui.TEXTO2));
        }
        for (Tablet t : ts) {
            LinearLayout c = Ui.tarjeta(this);
            c.addView(Ui.tv(this, t.serie, 18, true, Ui.TITULO));
            c.addView(Ui.tv(this, t.procesador + " · " + t.ram + " · " + t.almacenamiento, 13, false, Ui.TEXTO2));
            LinearLayout.LayoutParams lp = Ui.lp(this, 8);
            lp.width = -2;
            c.addView(Ui.chip(this, t.estado.etiqueta, t.estado.color), lp);
            c.setOnClickListener(v -> {
                Intent i = new Intent(this, FichaActivity.class);
                i.putExtra("serie", t.serie);
                startActivity(i);
            });
            content.addView(c, Ui.lp(this, 10));
        }
    }
}
