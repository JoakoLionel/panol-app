package ar.edu.et35.panol;

import android.content.Intent;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.List;

import ar.edu.et35.panol.Modelo.Empleado;
import ar.edu.et35.panol.Modelo.Res;
import ar.edu.et35.panol.Modelo.Tarea;

/** Alta de empleados (legajos) y asignación de tareas (con autorización o no de Home Office). */
public class EmpleadosActivity extends BaseActivity {
    private LinearLayout lista;

    @Override protected String titulo() { return "Empleados y tareas"; }

    @Override protected void construir() {
        content.addView(Ui.boton(this, "+ Nuevo empleado", Ui.PRIMARIO, v -> dialogoEmpleado()));
        content.addView(Ui.boton(this, "+ Asignar tarea", Ui.SECUNDARIO, v -> dialogoTarea()), Ui.lp(this, 10));
        lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        content.addView(lista, Ui.lp(this, 14));
        refrescar();
    }

    private void refrescar() {
        lista.removeAllViews();
        for (Empleado e : db.empleados()) {
            LinearLayout c = Ui.tarjeta(this);
            c.addView(Ui.tv(this, e.legajo + " · " + e.nombre, 17, true, Ui.TITULO));
            c.addView(Ui.tv(this, e.area == null || e.area.isEmpty() ? "Sin área" : e.area, 13, false, Ui.TEXTO2));
            List<Tarea> ts = db.tareasActivas(e.legajo);
            if (ts.isEmpty()) c.addView(Ui.tv(this, "Sin tareas activas", 14, false, 0xFFC62828), Ui.lp(this, 8));
            for (Tarea t : ts) {
                LinearLayout f = new LinearLayout(this);
                f.setOrientation(LinearLayout.HORIZONTAL);
                f.addView(Ui.tv(this, "• " + t.toString(), 14, false, Ui.TEXTO),
                        new LinearLayout.LayoutParams(0, -2, 1f));
                f.addView(Ui.boton(this, "Cerrar", Ui.BTN_GRIS, v -> {
                    db.cerrarTarea(t.id);
                    refrescar();
                }));
                c.addView(f, Ui.lp(this, 6));
            }
            LinearLayout acc = new LinearLayout(this);
            acc.setOrientation(LinearLayout.HORIZONTAL);
            acc.addView(Ui.boton(this, "Ver historial", Ui.SECUNDARIO, v -> abrirHistorial(e)),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            acc.addView(Ui.boton(this, "Eliminar", 0xFFC62828, v -> confirmarEliminar(e)),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            c.addView(acc, Ui.lp(this, 12));
            lista.addView(c, Ui.lp(this, 10));
        }
        List<Empleado> baja = db.empleadosDesactivados();
        if (!baja.isEmpty()) {
            lista.addView(Ui.tv(this, "Desactivados (conservan su historial)", 15, true, Ui.TEXTO2), Ui.lp(this, 22));
            for (Empleado e : baja) {
                LinearLayout c = Ui.tarjeta(this);
                c.addView(Ui.tv(this, e.legajo + " · " + e.nombre, 17, true, Ui.TEXTO2));
                c.addView(Ui.tv(this, e.area == null || e.area.isEmpty() ? "Sin área" : e.area, 13, false, Ui.TEXTO2));
                LinearLayout acc = new LinearLayout(this);
                acc.setOrientation(LinearLayout.HORIZONTAL);
                acc.addView(Ui.boton(this, "Ver historial", Ui.SECUNDARIO, v -> abrirHistorial(e)),
                        new LinearLayout.LayoutParams(0, -2, 1f));
                acc.addView(Ui.boton(this, "Reactivar", 0xFF2E7D32, v -> {
                    Res r = db.reactivarEmpleado(e.legajo);
                    toast(r.msg);
                    refrescar();
                }), new LinearLayout.LayoutParams(0, -2, 1f));
                c.addView(acc, Ui.lp(this, 12));
                lista.addView(c, Ui.lp(this, 10));
            }
        }
    }

    private void abrirHistorial(Empleado e) {
        Intent i = new Intent(this, HistorialEmpleadoActivity.class);
        i.putExtra("legajo", e.legajo);
        startActivity(i);
    }

    private void confirmarEliminar(Empleado e) {
        dialogo().setTitle("Eliminar empleado")
                .setMessage("¿Eliminar a " + e.nombre + " (legajo " + e.legajo + ")?\n\n"
                        + "• Si nunca retiró una tablet, se borra del todo.\n"
                        + "• Si ya tiene historial, queda desactivado y su historial se conserva.\n"
                        + "• Si tiene una tablet en su poder, no se puede eliminar.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    Res r = db.eliminarEmpleado(e.legajo);
                    alerta(r.ok ? "Listo" : "No se pudo eliminar", r.msg, r.ok ? Ui.SECUNDARIO : 0xFFC62828);
                    refrescar();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private EditText campo(LinearLayout l, String hint, boolean numero) {
        EditText e = new EditText(this);
        e.setHint(hint);
        if (numero) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        l.addView(e);
        return e;
    }

    private void dialogoEmpleado() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        l.setPadding(p, p / 2, p, 0);
        EditText leg = campo(l, "Legajo (número)", true);
        EditText nom = campo(l, "Nombre y apellido", false);
        EditText area = campo(l, "Área", false);
        dialogo().setTitle("Nuevo empleado").setView(l)
                .setPositiveButton("Guardar", (d, w) -> {
                    Res r = db.nuevoEmpleado(leg.getText().toString(), nom.getText().toString(), area.getText().toString());
                    if (!r.ok) alerta("No se pudo guardar", r.msg, 0xFFC62828);
                    refrescar();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void dialogoTarea() {
        List<Empleado> emps = db.empleados();
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        l.setPadding(p, p / 2, p, 0);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, emps));
        l.addView(sp);
        EditText desc = campo(l, "Descripción de la tarea", false);
        CheckBox ho = new CheckBox(this);
        ho.setText("Autorizada para Home Office");
        l.addView(ho);
        dialogo().setTitle("Asignar tarea").setView(l)
                .setPositiveButton("Guardar", (d, w) -> {
                    if (emps.isEmpty()) return;
                    Res r = db.nuevaTarea(emps.get(sp.getSelectedItemPosition()).legajo,
                            desc.getText().toString(), ho.isChecked());
                    if (!r.ok) alerta("No se pudo guardar", r.msg, 0xFFC62828);
                    refrescar();
                })
                .setNegativeButton("Cancelar", null).show();
    }
}
