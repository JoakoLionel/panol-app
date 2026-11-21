package ar.edu.et35.panol;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import ar.edu.et35.panol.Modelo.Empleado;

/** Base común: cabecera, base de datos, escaneo de QR y flujo alternativo por teclado. */
public abstract class BaseActivity extends AppCompatActivity {
    public interface TabletCb { void on(Tablet t, boolean registrada); }

    protected Db db;
    protected LinearLayout content;
    private TabletCb pendiente;

    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), r -> {
                if (r.getContents() == null) {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                    return;
                }
                procesarQr(r.getContents());
            });

    protected abstract String titulo();

    protected abstract void construir();

    private int versionTema;

    /** Diálogos con el aspecto claro u oscuro del tema elegido. */
    protected AlertDialog.Builder dialogo() {
        return new AlertDialog.Builder(this, Tema.oscuro()
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
    }

    @Override protected void onResume() {
        super.onResume();
        if (versionTema != Tema.version) recreate(); // se cambió el color de la app desde otra pantalla
    }

    @Override protected void onCreate(Bundle b) {
        Tema.cargar(this);
        versionTema = Tema.version;
        setTheme(Tema.oscuro() ? R.style.Theme_Panol_Dark : R.style.Theme_Panol);
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.CABECERA);
        db = Db.get(this);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(Ui.FONDO);

        TextView cab = Ui.tv(this, titulo(), 20, true, Color.WHITE);
        cab.setBackgroundColor(Ui.PRIMARIO);
        cab.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));
        raiz.addView(cab);

        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16);
        content.setPadding(p, p, p, p * 2);
        sv.addView(content);
        raiz.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(raiz);
        construir();
    }

    // ---------------------------------------------------------- Escaneo / QR

    protected void escanear(TabletCb cb) {
        pendiente = cb;
        ScanOptions o = new ScanOptions();
        o.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        o.setPrompt("Apuntá la cámara al código QR de la tablet");
        o.setBeepEnabled(true);
        o.setOrientationLocked(false);
        scanner.launch(o);
    }

    private void procesarQr(String raw) {
        try {
            Tablet leida = QrCodec.parse(raw);
            Tablet reg = db.tablet(leida.serie);
            if (pendiente != null) pendiente.on(reg != null ? reg : leida, reg != null);
        } catch (IllegalArgumentException e) {
            dialogo()
                    .setTitle("QR ilegible o inválido")
                    .setMessage(e.getMessage() + "\n\n¿Querés ingresar el número de serie por teclado?")
                    .setPositiveButton("Ingresar manualmente", (d, w) -> ingresoManual(pendiente))
                    .setNegativeButton("Reintentar escaneo", (d, w) -> escanear(pendiente))
                    .show();
        }
    }

    /** Flujo alternativo: QR dañado → se busca la tablet por su número de serie impreso/anotado. */
    protected void ingresoManual(TabletCb cb) {
        EditText et = new EditText(this);
        et.setHint("Ej: TAB-0001");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        dialogo()
                .setTitle("Ingreso manual del número de serie")
                .setView(et)
                .setPositiveButton("Buscar", (d, w) -> {
                    String s = QrCodec.normalizarSerie(et.getText().toString());
                    Tablet t = db.tablet(s);
                    if (t == null) {
                        alerta("No encontrada", "No hay ninguna tablet registrada con el número de serie \""
                                + s + "\". Si es nueva, regístrela desde \"Alta de tablet\" cargando los datos a mano.", Ui.PRIMARIO);
                    } else {
                        cb.on(t, true);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------------------------------------------------------------- Vistas

    protected View fichaTablet(Tablet t, boolean registrada) {
        LinearLayout c = Ui.tarjeta(this);
        c.addView(Ui.tv(this, t.serie, 22, true, Ui.TITULO));
        TextView chip = registrada
                ? Ui.chip(this, t.estado.etiqueta, t.estado.color)
                : Ui.chip(this, "NO REGISTRADA EN EL PAÑOL", 0xFF6D4C41);
        LinearLayout.LayoutParams lp = Ui.lp(this, 8);
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        c.addView(chip, lp);
        c.addView(Ui.tv(this, "Ficha técnica", 13, true, Ui.TEXTO2), Ui.lp(this, 14));
        c.addView(Ui.fila(this, "Procesador", t.procesador));
        c.addView(Ui.fila(this, "Memoria RAM", t.ram));
        c.addView(Ui.fila(this, "Almacenamiento", t.almacenamiento));
        c.addView(Ui.fila(this, "Pantalla", t.pantalla));
        c.addView(Ui.fila(this, "Puertos", t.puertos));
        c.addView(Ui.fila(this, "Conectividad", t.conectividad));
        if (registrada) {
            if (t.legajoActual != 0) {
                Empleado e = db.empleado(t.legajoActual);
                c.addView(Ui.fila(this, "En poder de",
                        t.legajoActual + (e != null ? " - " + e.nombre : "")));
            }
            c.addView(Ui.fila(this, "Alta en pañol", t.fechaAlta));
        }
        return c;
    }

    protected void alerta(String titulo, String msg, int color) {
        TextView t = Ui.tv(this, titulo, 19, true, Color.WHITE);
        t.setBackgroundColor(color);
        t.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 16));
        dialogo().setCustomTitle(t).setMessage(msg)
                .setPositiveButton("Entendido", null).show();
    }

    protected void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
