package ar.edu.et35.panol;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import ar.edu.et35.panol.Modelo.Res;

/** CU-03: recibir devolución, verificar estado físico y reingresar al pañol (o derivar a servicio técnico). */
public class DevolucionActivity extends BaseActivity {
    private LinearLayout zona;
    private RadioGroup rg;
    private EditText obs;
    private Tablet tablet;

    @Override protected String titulo() { return "Recibir devolución"; }

    @Override protected void construir() {
        content.addView(Ui.boton(this, "📷  Escanear QR de la tablet", Ui.PRIMARIO, v -> escanear(this::elegida)));
        content.addView(Ui.boton(this, "⌨  QR dañado: ingresar serie a mano", Ui.BTN_GRIS, v -> ingresoManual(this::elegida)),
                Ui.lp(this, 10));
        zona = new LinearLayout(this);
        zona.setOrientation(LinearLayout.VERTICAL);
        content.addView(zona, Ui.lp(this, 16));
    }

    private void elegida(Tablet t, boolean registrada) {
        zona.removeAllViews();
        zona.addView(fichaTablet(t, registrada));
        if (!registrada) {
            alerta("Tablet no registrada", "Esta tablet no figura en el sistema. Regístrela primero (CU-01).", 0xFFC62828);
            return;
        }
        tablet = t;
        if (t.estado == Estado.SERVICIO_TECNICO) {
            zona.addView(Ui.tv(this, "Esta tablet está en Servicio Técnico. Si ya fue reparada o actualizada, reingrésela al pañol.",
                    14, false, Ui.TEXTO), Ui.lp(this, 12));
            obs = new EditText(this);
            obs.setHint("Detalle del servicio realizado");
            zona.addView(obs);
            zona.addView(Ui.boton(this, "Finalizar servicio y reingresar al pañol", 0xFF2E7D32, v -> {
                Res r = db.finalizarServicio(tablet.serie, obs.getText().toString().trim());
                resultado(r);
            }), Ui.lp(this, 12));
            return;
        }
        if (!t.estado.enUso()) {
            alerta("Nada que devolver", Reglas.validarDevolucion(t.estado), 0xFFC62828);
            return;
        }
        zona.addView(Ui.tv(this, "Verificación del estado físico", 14, true, Ui.TEXTO2), Ui.lp(this, 16));
        rg = new RadioGroup(this);
        RadioButton ok = new RadioButton(this); ok.setText("Buen estado (pantalla, carcasa, cargador, puertos)"); ok.setId(1);
        RadioButton mal = new RadioButton(this); mal.setText("Con daños o falla → Servicio Técnico"); mal.setId(2);
        rg.addView(ok); rg.addView(mal); rg.check(1);
        zona.addView(rg);
        obs = new EditText(this);
        obs.setHint("Observaciones (obligatorio si hay daños)");
        zona.addView(obs, Ui.lp(this, 8));
        zona.addView(Ui.boton(this, "Confirmar devolución", 0xFF2E7D32, v -> {
            Res r = db.devolver(tablet.serie, rg.getCheckedRadioButtonId() == 2, obs.getText().toString().trim());
            resultado(r);
        }), Ui.lp(this, 14));
    }

    private void resultado(Res r) {
        if (!r.ok) { alerta("No se pudo registrar", r.msg, 0xFFC62828); return; }
        dialogo().setTitle("Devolución registrada").setMessage(r.msg)
                .setPositiveButton("Aceptar", (d, w) -> finish()).setCancelable(false).show();
    }
}
