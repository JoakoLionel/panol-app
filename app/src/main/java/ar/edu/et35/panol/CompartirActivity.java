package ar.edu.et35.panol;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ar.edu.et35.panol.Modelo.Res;

/** Compartir TODA la información de este celular con otros y combinar la que llega (archivo de datos). */
public class CompartirActivity extends BaseActivity {
    private static final int MAX_BYTES = 20 * 1024 * 1024;

    private final ActivityResultLauncher<String[]> selector =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::leerArchivo);

    @Override protected String titulo() { return "Compartir datos"; }

    @Override protected void construir() {
        LinearLayout c = Ui.tarjeta(this);
        c.addView(Ui.tv(this, "Cómo funciona", 15, true, Ui.TITULO));
        c.addView(Ui.tv(this, "1. En el celular que tiene los datos: tocá \"Enviar mis datos\" y mandá el archivo por "
                + "WhatsApp, Drive, correo o Bluetooth.\n2. En el otro celular: guardá el archivo y tocá \"Recibir datos\" "
                + "para elegirlo.\n\nSe comparte todo: tablets, empleados, tareas y movimientos. Los datos del otro celular "
                + "se COMBINAN con los tuyos: no se borra nada. Si una tablet tiene movimientos distintos, queda con el "
                + "estado del movimiento más reciente.", 14, false, Ui.TEXTO), Ui.lp(this, 8));
        content.addView(c);
        content.addView(Ui.boton(this, "📤  Enviar mis datos", Ui.PRIMARIO, v -> enviar()), Ui.lp(this, 16));
        content.addView(Ui.boton(this, "📥  Recibir datos (elegir archivo)", Ui.SECUNDARIO,
                v -> selector.launch(new String[]{"*/*"})), Ui.lp(this, 10));
    }

    private void enviar() {
        try {
            String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            String json = Intercambio.toJson(db.exportar(), fecha);
            File dir = new File(getCacheDir(), "compartir");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("No se pudo crear la carpeta temporal");
            File[] viejos = dir.listFiles();
            if (viejos != null) for (File f : viejos) //noinspection ResultOfMethodCallIgnored
                f.delete();
            File f = new File(dir, "panol_datos_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".json");
            try (FileOutputStream os = new FileOutputStream(f)) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/json");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, "Datos de Pañol-App");
            i.setClipData(ClipData.newRawUri("datos", uri));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Enviar datos de Pañol-App"));
        } catch (Exception e) {
            alerta("No se pudo preparar el archivo", String.valueOf(e.getMessage()), 0xFFC62828);
        }
    }

    private void leerArchivo(Uri uri) {
        if (uri == null) return; // el usuario canceló
        Intercambio.Snapshot sn;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalArgumentException("No se pudo abrir el archivo.");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n; int total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_BYTES) throw new IllegalArgumentException("El archivo es demasiado grande.");
                bo.write(buf, 0, n);
            }
            sn = Intercambio.parse(new String(bo.toByteArray(), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            alerta("Archivo no válido", e.getMessage(), 0xFFC62828);
            return;
        } catch (Exception e) {
            alerta("No se pudo leer el archivo", String.valueOf(e.getMessage()), 0xFFC62828);
            return;
        }
        int[] nuevo = db.contarNuevos(sn);
        String resumen = "El archivo trae " + sn.tablets.size() + " tablets, " + sn.empleados.size() + " empleados, "
                + sn.tareas.size() + " tareas y " + sn.movimientos.size() + " movimientos.\n\nDe esos, son NUEVOS para este celular: "
                + nuevo[0] + " tablets, " + nuevo[1] + " empleados, " + nuevo[2] + " tareas y " + nuevo[3]
                + " movimientos.\n\n¿Combinarlos con tus datos? No se borra nada.";
        final Intercambio.Snapshot snap = sn;
        dialogo().setTitle("Recibir datos").setMessage(resumen)
                .setPositiveButton("Combinar", (d, w) -> {
                    Res r = db.importar(snap);
                    alerta(r.ok ? "Datos combinados" : "No se pudo importar", r.msg, r.ok ? Ui.SECUNDARIO : 0xFFC62828);
                })
                .setNegativeButton("Cancelar", null).show();
    }
}
