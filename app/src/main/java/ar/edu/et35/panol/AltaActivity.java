package ar.edu.et35.panol;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

import ar.edu.et35.panol.Modelo.Res;

/** CU-01: alta de tablet en el pañol y generación lógica del QR. */
public class AltaActivity extends BaseActivity {
    private EditText serie, cpu, ram, alm, pant, puertos, conect;
    private LinearLayout salida;

    @Override protected String titulo() { return "Alta de tablet en el pañol"; }

    private EditText campo(String hint, String etiqueta) {
        content.addView(Ui.tv(this, etiqueta, 13, true, Ui.TEXTO2), Ui.lp(this, 10));
        EditText e = new EditText(this);
        e.setHint(hint); e.setSingleLine(true);
        e.setBackground(Ui.fondo(this, Ui.INPUT, 8));
        e.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        content.addView(e, Ui.lp(this, 4));
        return e;
    }

    @Override protected void construir() {
        content.addView(Ui.boton(this, "📷  Precargar datos escaneando un QR", Ui.PRIMARIO,
                v -> escanear((t, reg) -> {
                    if (reg) alerta("Ya registrada", "La tablet " + t.serie + " ya está dada de alta.", Ui.SECUNDARIO);
                    else cargar(t);
                })));
        serie = campo("TAB-0001", "Número de serie *");
        cpu = campo("Snapdragon 680", "Procesador *");
        ram = campo("4 GB", "Memoria RAM");
        alm = campo("64 GB", "Almacenamiento");
        pant = campo("10.1\"", "Tamaño de pantalla");
        puertos = campo("1x USB-C, 1x Jack 3.5", "Puertos");
        conect = campo("Wi-Fi, 4G, Bluetooth", "Conectividad");
        content.addView(Ui.boton(this, "Registrar y generar QR", 0xFF6A4C93, v -> registrar()), Ui.lp(this, 18));
        salida = new LinearLayout(this);
        salida.setOrientation(LinearLayout.VERTICAL);
        content.addView(salida, Ui.lp(this, 12));

        String raw = getIntent().getStringExtra("raw");
        if (raw != null) {
            try { cargar(QrCodec.parse(raw)); } catch (IllegalArgumentException ignored) {}
        }
    }

    private void cargar(Tablet t) {
        serie.setText(t.serie); cpu.setText(t.procesador); ram.setText(t.ram); alm.setText(t.almacenamiento);
        pant.setText(t.pantalla); puertos.setText(t.puertos); conect.setText(t.conectividad);
    }

    private void registrar() {
        Tablet t = new Tablet();
        t.serie = serie.getText().toString(); t.procesador = cpu.getText().toString().trim();
        t.ram = ram.getText().toString().trim(); t.almacenamiento = alm.getText().toString().trim();
        t.pantalla = pant.getText().toString().trim(); t.puertos = puertos.getText().toString().trim();
        t.conectividad = conect.getText().toString().trim();
        Res r = db.alta(t);
        if (!r.ok) { alerta("No se pudo registrar", r.msg, 0xFFC62828); return; }
        t = db.tablet(t.serie);
        final Bitmap qr = generarQr(QrCodec.toJson(t), 700);
        final String nombre = "QR_" + t.serie;
        salida.removeAllViews();
        salida.addView(Ui.tv(this, "✔ " + r.msg, 16, true, 0xFF2E7D32));
        LinearLayout c = Ui.tarjeta(this);
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(qr);
        iv.setAdjustViewBounds(true);
        c.addView(iv, new LinearLayout.LayoutParams(-1, -2));
        c.addView(Ui.tv(this, t.serie, 18, true, Ui.TITULO), Ui.lp(this, 6));
        salida.addView(c, Ui.lp(this, 10));
        salida.addView(Ui.boton(this, "Guardar QR en la galería (para imprimir)", Ui.SECUNDARIO,
                v -> guardar(qr, nombre)), Ui.lp(this, 10));
    }

    static Bitmap generarQr(String texto, int px) {
        try {
            Map<EncodeHintType, Object> h = new EnumMap<>(EncodeHintType.class);
            h.put(EncodeHintType.MARGIN, 2);
            h.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, px, px, h);
            int[] px_ = new int[px * px];
            for (int y = 0; y < px; y++)
                for (int x = 0; x < px; x++)
                    px_[y * px + x] = m.get(x, y) ? Color.BLACK : Color.WHITE;
            Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            b.setPixels(px_, 0, px, 0, 0, px, px);
            return b;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void guardar(Bitmap b, String nombre) {
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, nombre + ".png");
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= 29) v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PanolApp");
            Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new IllegalStateException("sin destino");
            try (OutputStream os = getContentResolver().openOutputStream(u)) {
                b.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            toast("QR guardado en Imágenes/PanolApp");
        } catch (Exception e) {
            toast("No se pudo guardar el QR: " + e.getMessage());
        }
    }
}
