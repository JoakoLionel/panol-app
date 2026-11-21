package ar.edu.et35.panol;

import android.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ar.edu.et35.panol.Modelo.Empleado;
import ar.edu.et35.panol.Modelo.Res;
import ar.edu.et35.panol.Modelo.Tarea;

/** CU-02: despachar tablet (uso local u home office) asociada a un legajo y a una tarea. */
public class DespachoActivity extends BaseActivity {
    private Tablet tablet;
    private LinearLayout zona;
    private Spinner spEmp, spTarea;
    private RadioGroup rgUso;
    private TextView aviso;
    private List<Empleado> empleados;
    private List<Tarea> tareas = new ArrayList<>();

    @Override protected String titulo() { return "Despachar tablet"; }

    @Override protected void construir() {
        content.addView(Ui.boton(this, "📷  1. Escanear QR de la tablet", Ui.PRIMARIO, v -> escanear(this::elegida)));
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
            alerta("Tablet no registrada", "Esta tablet no está en el pañol. Primero debe darse de alta (CU-01).", 0xFFC62828);
            return;
        }
        if (t.estado != Estado.EN_PANOL) {
            alerta("No disponible", Reglas.validarDespacho(t.estado, Reglas.Uso.LOCAL, true, true), 0xFFC62828);
            return;
        }
        tablet = t;
        zona.addView(Ui.tv(this, "2. Empleado (legajo)", 14, true, Ui.TEXTO2), Ui.lp(this, 16));
        empleados = db.empleados();
        spEmp = new Spinner(this);
        spEmp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, empleados));
        zona.addView(spEmp);

        zona.addView(Ui.tv(this, "3. Tipo de uso", 14, true, Ui.TEXTO2), Ui.lp(this, 12));
        rgUso = new RadioGroup(this);
        RadioButton rl = new RadioButton(this); rl.setText("Uso local (oficina) · se devuelve al terminar la jornada"); rl.setId(1);
        RadioButton rx = new RadioButton(this); rx.setText("Home Office (se la lleva a su casa)"); rx.setId(2);
        rgUso.addView(rl); rgUso.addView(rx);
        rgUso.check(1);
        zona.addView(rgUso);

        zona.addView(Ui.tv(this, "4. Tarea asociada", 14, true, Ui.TEXTO2), Ui.lp(this, 12));
        spTarea = new Spinner(this);
        zona.addView(spTarea);

        aviso = Ui.tv(this, "", 14, true, 0xFFFFFFFF);
        aviso.setBackground(Ui.fondo(this, 0xFFC62828, 8));
        aviso.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        aviso.setVisibility(View.GONE);
        zona.addView(aviso, Ui.lp(this, 10));

        zona.addView(Ui.boton(this, "Confirmar despacho", 0xFF2E7D32, v -> confirmar()), Ui.lp(this, 16));

        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (p == spEmp) cargarTareas();
                evaluar();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spEmp.setOnItemSelectedListener(l);
        spTarea.setOnItemSelectedListener(l);
        rgUso.setOnCheckedChangeListener((g, id) -> evaluar());
        cargarTareas();
        evaluar();
    }

    private Empleado empleadoSel() {
        return empleados.isEmpty() ? null : empleados.get(spEmp.getSelectedItemPosition());
    }

    private void cargarTareas() {
        Empleado e = empleadoSel();
        tareas = e == null ? new ArrayList<>() : db.tareasActivas(e.legajo);
        List<String> txt = new ArrayList<>();
        txt.add("— Sin tarea asociada —");
        for (Tarea t : tareas) txt.add(t.toString());
        spTarea.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, txt));
    }

    private Reglas.Uso uso() { return rgUso.getCheckedRadioButtonId() == 2 ? Reglas.Uso.EXTERNO : Reglas.Uso.LOCAL; }

    private Tarea tareaSel() {
        int p = spTarea.getSelectedItemPosition();
        return p <= 0 || p > tareas.size() ? null : tareas.get(p - 1);
    }

    /** Alerta visual inmediata: Home Office sin tarea que lo justifique. */
    private void evaluar() {
        if (aviso == null || spTarea == null || spEmp == null) return;
        Tarea t = tareaSel();
        boolean ok = t != null && t.homeOffice;
        if (uso() == Reglas.Uso.EXTERNO && !ok) {
            aviso.setText("⚠ Home Office sin tarea autorizada: elegí una tarea habilitada para Home Office o el sistema rechazará el retiro.");
            aviso.setVisibility(View.VISIBLE);
        } else {
            aviso.setVisibility(View.GONE);
        }
    }

    private void confirmar() {
        Empleado e = empleadoSel();
        Tarea t = tareaSel();
        Res r = db.despachar(tablet.serie, e == null ? 0 : e.legajo, uso(), t == null ? 0 : t.id);
        if (!r.ok) {
            alerta(r.msg.startsWith("ALERTA") ? "⚠ Retiro Home Office rechazado" : "Despacho rechazado", r.msg, 0xFFC62828);
            return;
        }
        dialogo().setTitle("Despacho registrado").setMessage(r.msg)
                .setPositiveButton("Aceptar", (d, w) -> finish()).setCancelable(false).show();
    }
}
