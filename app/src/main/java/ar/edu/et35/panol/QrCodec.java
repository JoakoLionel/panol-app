package ar.edu.et35.panol;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Codifica / decodifica el contenido del QR pegado en cada tablet.
 *
 * Formato principal (JSON):
 *   {"sn":"TAB-0001","cpu":"Snapdragon 680","ram":"4 GB","alm":"64 GB",
 *    "pantalla":"10.1\"","puertos":"1x USB-C, 1x Jack 3.5","conectividad":"Wi-Fi, 4G, Bluetooth"}
 *
 * Formato alternativo (texto delimitado por ';' o '|', 7 campos en el mismo orden):
 *   TAB-0001;Snapdragon 680;4 GB;64 GB;10.1";1x USB-C, 1x Jack 3.5;Wi-Fi, 4G, Bluetooth
 */
public final class QrCodec {
    private QrCodec() {}

    public static String toJson(Tablet t) {
        try {
            JSONObject o = new JSONObject();
            o.put("sn", t.serie);
            o.put("cpu", t.procesador);
            o.put("ram", t.ram);
            o.put("alm", t.almacenamiento);
            o.put("pantalla", t.pantalla);
            o.put("puertos", t.puertos);
            o.put("conectividad", t.conectividad);
            return o.toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @throws IllegalArgumentException si el contenido no representa una tablet válida. */
    public static Tablet parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("El código está vacío.");
        }
        String s = raw.trim();
        Tablet t = new Tablet();
        if (s.startsWith("{")) {
            try {
                JSONObject o = new JSONObject(s);
                t.serie = o.optString("sn", "");
                t.procesador = o.optString("cpu", "");
                t.ram = o.optString("ram", "");
                t.almacenamiento = o.optString("alm", "");
                t.pantalla = o.optString("pantalla", "");
                t.puertos = o.optString("puertos", "");
                t.conectividad = o.optString("conectividad", "");
            } catch (JSONException e) {
                throw new IllegalArgumentException("El JSON del QR está dañado o incompleto.");
            }
        } else {
            String[] p = s.split("[;|]", -1);
            if (p.length != 7) {
                throw new IllegalArgumentException(
                        "El QR no tiene el formato esperado (se necesitan 7 campos, vinieron " + p.length + ").");
            }
            t.serie = p[0];
            t.procesador = p[1];
            t.ram = p[2];
            t.almacenamiento = p[3];
            t.pantalla = p[4];
            t.puertos = p[5];
            t.conectividad = p[6];
        }
        t.serie = normalizarSerie(t.serie);
        t.procesador = t.procesador.trim();
        t.ram = t.ram.trim();
        t.almacenamiento = t.almacenamiento.trim();
        t.pantalla = t.pantalla.trim();
        t.puertos = t.puertos.trim();
        t.conectividad = t.conectividad.trim();
        if (t.serie.isEmpty()) {
            throw new IllegalArgumentException("El QR no contiene número de serie.");
        }
        return t;
    }

    public static String normalizarSerie(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}
