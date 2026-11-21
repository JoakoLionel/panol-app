package ar.edu.et35.panol;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Formato del archivo para compartir datos entre celulares (JSON) y reglas para reconstruir el estado de
 * una tablet a partir de su historial. No depende de Android, así que se prueba con JUnit.
 */
public final class Intercambio {
    private Intercambio() {}

    public static final String APP = "panol-app";
    public static final int VERSION = 1;
    private static final int MAX_FILAS = 200000, MAX_TEXTO = 1000;
    private static final Set<String> TIPOS = new HashSet<>(Arrays.asList("ALTA", "DESPACHO", "DEVOLUCION", "FIN_SERVICIO"));

    public static class Emp { public int legajo; public String nombre = "", area = ""; public boolean activo = true; }
    public static class Tar { public String uid = "", descripcion = "", fecha = ""; public int legajo; public boolean homeOffice, activa; }
    public static class Tab { public String serie = "", procesador = "", ram = "", almacenamiento = "", pantalla = "",
            puertos = "", conectividad = "", fechaAlta = ""; }
    public static class Mov { public String uid = "", serie = "", tipo = "", origen, destino = "", tareaUid,
            estadoFisico, observaciones, fecha = ""; public int legajo; /* 0 = ninguno */ }

    public static class Snapshot {
        public List<Emp> empleados = new ArrayList<>();
        public List<Tar> tareas = new ArrayList<>();
        public List<Tab> tablets = new ArrayList<>();
        public List<Mov> movimientos = new ArrayList<>();
    }

    // ------------------------------------------------------------ Exportar

    public static String toJson(Snapshot s, String fecha) {
        try {
            JSONObject o = new JSONObject();
            o.put("app", APP); o.put("version", VERSION); o.put("exportado", fecha);
            JSONArray e = new JSONArray();
            for (Emp x : s.empleados) e.put(new JSONObject().put("legajo", x.legajo).put("nombre", x.nombre)
                    .put("area", x.area).put("activo", x.activo ? 1 : 0));
            o.put("empleados", e);
            JSONArray t = new JSONArray();
            for (Tar x : s.tareas) t.put(new JSONObject().put("uid", x.uid).put("legajo", x.legajo)
                    .put("descripcion", x.descripcion).put("home_office", x.homeOffice ? 1 : 0)
                    .put("activa", x.activa ? 1 : 0).put("fecha", x.fecha));
            o.put("tareas", t);
            JSONArray b = new JSONArray();
            for (Tab x : s.tablets) b.put(new JSONObject().put("serie", x.serie).put("procesador", x.procesador)
                    .put("ram", x.ram).put("almacenamiento", x.almacenamiento).put("pantalla", x.pantalla)
                    .put("puertos", x.puertos).put("conectividad", x.conectividad).put("fecha_alta", x.fechaAlta));
            o.put("tablets", b);
            JSONArray m = new JSONArray();
            for (Mov x : s.movimientos) {
                JSONObject j = new JSONObject().put("uid", x.uid).put("serie", x.serie).put("tipo", x.tipo)
                        .put("estado_destino", x.destino).put("fecha", x.fecha);
                if (x.origen != null) j.put("estado_origen", x.origen);
                if (x.legajo != 0) j.put("legajo", x.legajo);
                if (x.tareaUid != null) j.put("tarea_uid", x.tareaUid);
                if (x.estadoFisico != null) j.put("estado_fisico", x.estadoFisico);
                if (x.observaciones != null) j.put("observaciones", x.observaciones);
                m.put(j);
            }
            o.put("movimientos", m);
            return o.toString();
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ------------------------------------------------------------ Importar

    private static String txt(JSONObject o, String k, boolean obligatorio) {
        String v = o.isNull(k) ? null : o.optString(k, null);
        if (v == null || v.isEmpty()) {
            if (obligatorio) throw new IllegalArgumentException("Falta el campo \"" + k + "\".");
            return null;
        }
        if (v.length() > MAX_TEXTO) throw new IllegalArgumentException("El campo \"" + k + "\" es demasiado largo.");
        return v;
    }

    private static String txtVacio(JSONObject o, String k) {
        String v = txt(o, k, false);
        return v == null ? "" : v;
    }

    /** @throws IllegalArgumentException si el archivo no es un archivo de datos válido de Pañol-App. */
    public static Snapshot parse(String json) {
        JSONObject o;
        try {
            o = new JSONObject(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException("El archivo no es un archivo de datos de Pañol-App (JSON inválido).");
        }
        if (!APP.equals(o.optString("app"))) {
            throw new IllegalArgumentException("El archivo no pertenece a Pañol-App.");
        }
        if (o.optInt("version", 0) < 1 || o.optInt("version", 0) > VERSION) {
            throw new IllegalArgumentException("Versión de archivo no compatible: actualizá la app en este celular.");
        }
        Snapshot s = new Snapshot();
        try {
            JSONArray e = o.optJSONArray("empleados"), t = o.optJSONArray("tareas"),
                    b = o.optJSONArray("tablets"), m = o.optJSONArray("movimientos");
            if (e == null || t == null || b == null || m == null)
                throw new IllegalArgumentException("Al archivo le faltan secciones de datos.");
            if (e.length() > MAX_FILAS || t.length() > MAX_FILAS || b.length() > MAX_FILAS || m.length() > MAX_FILAS)
                throw new IllegalArgumentException("El archivo es demasiado grande.");
            for (int i = 0; i < e.length(); i++) {
                JSONObject j = e.getJSONObject(i); Emp x = new Emp();
                x.legajo = j.getInt("legajo");
                if (x.legajo <= 0) throw new IllegalArgumentException("Legajo inválido: " + x.legajo);
                x.nombre = txt(j, "nombre", true); x.area = txtVacio(j, "area"); x.activo = j.optInt("activo", 1) == 1;
                s.empleados.add(x);
            }
            for (int i = 0; i < t.length(); i++) {
                JSONObject j = t.getJSONObject(i); Tar x = new Tar();
                x.uid = txt(j, "uid", true); x.legajo = j.getInt("legajo"); x.descripcion = txt(j, "descripcion", true);
                x.homeOffice = j.optInt("home_office", 0) == 1; x.activa = j.optInt("activa", 1) == 1;
                x.fecha = txt(j, "fecha", true);
                s.tareas.add(x);
            }
            for (int i = 0; i < b.length(); i++) {
                JSONObject j = b.getJSONObject(i); Tab x = new Tab();
                x.serie = QrCodec.normalizarSerie(txt(j, "serie", true)); x.procesador = txt(j, "procesador", true);
                x.ram = txtVacio(j, "ram"); x.almacenamiento = txtVacio(j, "almacenamiento");
                x.pantalla = txtVacio(j, "pantalla"); x.puertos = txtVacio(j, "puertos");
                x.conectividad = txtVacio(j, "conectividad"); x.fechaAlta = txt(j, "fecha_alta", true);
                s.tablets.add(x);
            }
            for (int i = 0; i < m.length(); i++) {
                JSONObject j = m.getJSONObject(i); Mov x = new Mov();
                x.uid = txt(j, "uid", true); x.serie = QrCodec.normalizarSerie(txt(j, "serie", true));
                x.tipo = txt(j, "tipo", true);
                if (!TIPOS.contains(x.tipo)) throw new IllegalArgumentException("Tipo de movimiento inválido: " + x.tipo);
                x.origen = txt(j, "estado_origen", false); x.destino = txt(j, "estado_destino", true);
                try {
                    if (x.origen != null) Estado.valueOf(x.origen);
                    Estado.valueOf(x.destino);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Estado inválido en el movimiento " + x.uid + ".");
                }
                x.legajo = j.optInt("legajo", 0); x.tareaUid = txt(j, "tarea_uid", false);
                x.estadoFisico = txt(j, "estado_fisico", false); x.observaciones = txt(j, "observaciones", false);
                x.fecha = txt(j, "fecha", true);
                s.movimientos.add(x);
            }
        } catch (JSONException ex) {
            throw new IllegalArgumentException("El archivo tiene datos mal formados.");
        }
        return s;
    }

    // ------------------------------------------- Estado a partir del historial

    public static class Ultimo {
        public final Estado estado; public final int legajo;
        Ultimo(Estado e, int l) { estado = e; legajo = l; }
    }

    /**
     * Estado actual de una tablet = destino de su último movimiento (por fecha; si empatan, el que figura después).
     * Si está en uso, el legajo es el del movimiento. Lista vacía → En Pañol.
     */
    public static Ultimo ultimoEstado(List<Mov> movs) {
        Mov ult = null;
        for (Mov m : movs) if (ult == null || m.fecha.compareTo(ult.fecha) >= 0) ult = m;
        if (ult == null) return new Ultimo(Estado.EN_PANOL, 0);
        Estado e = Estado.valueOf(ult.destino);
        return new Ultimo(e, e.enUso() ? ult.legajo : 0);
    }
}
