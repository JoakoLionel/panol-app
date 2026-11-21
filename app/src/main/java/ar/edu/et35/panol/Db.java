package ar.edu.et35.panol;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import ar.edu.et35.panol.Modelo.Empleado;
import ar.edu.et35.panol.Modelo.Movimiento;
import ar.edu.et35.panol.Modelo.Prestamo;
import ar.edu.et35.panol.Modelo.Res;
import ar.edu.et35.panol.Modelo.Tarea;

/** Persistencia local (SQLite). Toda operación que cambia estado deja un movimiento (trazabilidad). */
public class Db extends SQLiteOpenHelper {
    private static Db inst;

    public static synchronized Db get(Context c) {
        if (inst == null) inst = new Db(c.getApplicationContext());
        return inst;
    }

    private Db(Context c) { super(c, "panol.db", null, 3); }

    private static String ahora() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE empleado(legajo INTEGER PRIMARY KEY, nombre TEXT NOT NULL, area TEXT, "
                + "activo INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE tablet(serie TEXT PRIMARY KEY, procesador TEXT NOT NULL, ram TEXT, "
                + "almacenamiento TEXT, pantalla TEXT, puertos TEXT, conectividad TEXT, "
                + "estado TEXT NOT NULL CHECK(estado IN ('EN_PANOL','USO_LOCAL','USO_EXTERNO','SERVICIO_TECNICO')), "
                + "legajo_actual INTEGER REFERENCES empleado(legajo), fecha_alta TEXT NOT NULL)");
        db.execSQL("CREATE TABLE tarea(id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "legajo INTEGER NOT NULL REFERENCES empleado(legajo), descripcion TEXT NOT NULL, "
                + "home_office INTEGER NOT NULL DEFAULT 0, activa INTEGER NOT NULL DEFAULT 1, fecha TEXT NOT NULL, uid TEXT)");
        db.execSQL("CREATE TABLE movimiento(id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "serie TEXT NOT NULL REFERENCES tablet(serie), tipo TEXT NOT NULL, "
                + "estado_origen TEXT, estado_destino TEXT NOT NULL, legajo INTEGER, tarea_id INTEGER, "
                + "estado_fisico TEXT, observaciones TEXT, fecha TEXT NOT NULL, uid TEXT)");
        db.execSQL("CREATE UNIQUE INDEX idx_tarea_uid ON tarea(uid)");
        db.execSQL("CREATE UNIQUE INDEX idx_mov_uid ON movimiento(uid)");
        // Datos de ejemplo para la demostración
        emp(db, 1001, "Rubén Arias", "Sistemas");
        emp(db, 1002, "Ariel Quiroz", "Redes");
        emp(db, 1003, "Joaquín Ojeda", "Soporte técnico");
        emp(db, 1004, "Lucía Fernández", "Administración");
        tar(db, 1001, "Relevamiento en sucursal y carga remota de informes", 1, "seed-1");
        tar(db, 1002, "Mantenimiento de servidores (solo en sitio)", 0, "seed-2");
        tar(db, 1003, "Capacitación remota a clientes", 1, "seed-3");
    }

    private void emp(SQLiteDatabase db, int l, String n, String a) {
        ContentValues v = new ContentValues();
        v.put("legajo", l); v.put("nombre", n); v.put("area", a);
        db.insert("empleado", null, v);
    }

    private void tar(SQLiteDatabase db, int l, String d, int ho, String uid) {
        ContentValues v = new ContentValues();
        v.put("legajo", l); v.put("descripcion", d); v.put("home_office", ho); v.put("fecha", ahora()); v.put("uid", uid);
        db.insert("tarea", null, v);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) {
        // v1 -> v2: los empleados pueden desactivarse (conservan su historial). Los datos existentes se mantienen.
        if (o < 2) db.execSQL("ALTER TABLE empleado ADD COLUMN activo INTEGER NOT NULL DEFAULT 1");
        // v2 -> v3: cada tarea y movimiento recibe un identificador único para poder combinar datos entre celulares.
        if (o < 3) {
            db.execSQL("ALTER TABLE tarea ADD COLUMN uid TEXT");
            db.execSQL("ALTER TABLE movimiento ADD COLUMN uid TEXT");
            // las 3 tareas de ejemplo son iguales en todos los celulares: mismo identificador para no duplicarlas
            db.execSQL("UPDATE tarea SET uid='seed-'||id WHERE (id=1 AND legajo=1001) OR (id=2 AND legajo=1002) OR (id=3 AND legajo=1003)");
            db.execSQL("UPDATE tarea SET uid=lower(hex(randomblob(16))) WHERE uid IS NULL");
            db.execSQL("UPDATE movimiento SET uid=lower(hex(randomblob(16))) WHERE uid IS NULL");
            db.execSQL("CREATE UNIQUE INDEX idx_tarea_uid ON tarea(uid)");
            db.execSQL("CREATE UNIQUE INDEX idx_mov_uid ON movimiento(uid)");
        }
    }

    // ---------------------------------------------------------------- Tablets

    private Tablet leer(Cursor c) {
        Tablet t = new Tablet();
        t.serie = c.getString(c.getColumnIndexOrThrow("serie"));
        t.procesador = c.getString(c.getColumnIndexOrThrow("procesador"));
        t.ram = c.getString(c.getColumnIndexOrThrow("ram"));
        t.almacenamiento = c.getString(c.getColumnIndexOrThrow("almacenamiento"));
        t.pantalla = c.getString(c.getColumnIndexOrThrow("pantalla"));
        t.puertos = c.getString(c.getColumnIndexOrThrow("puertos"));
        t.conectividad = c.getString(c.getColumnIndexOrThrow("conectividad"));
        t.estado = Estado.valueOf(c.getString(c.getColumnIndexOrThrow("estado")));
        t.legajoActual = c.getInt(c.getColumnIndexOrThrow("legajo_actual"));
        t.fechaAlta = c.getString(c.getColumnIndexOrThrow("fecha_alta"));
        return t;
    }

    public Tablet tablet(String serie) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tablet WHERE serie=?",
                new String[]{QrCodec.normalizarSerie(serie)})) {
            return c.moveToFirst() ? leer(c) : null;
        }
    }

    public List<Tablet> tablets() {
        List<Tablet> l = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tablet ORDER BY serie", null)) {
            while (c.moveToNext()) l.add(leer(c));
        }
        return l;
    }

    public int contar(Estado e) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tablet WHERE estado=?",
                new String[]{e.name()})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** CU-01: alta en el pañol. */
    public Res alta(Tablet t) {
        t.serie = QrCodec.normalizarSerie(t.serie);
        if (t.serie.isEmpty()) return new Res(false, "El número de serie es obligatorio.");
        if (t.procesador.trim().isEmpty()) return new Res(false, "El procesador es obligatorio.");
        if (tablet(t.serie) != null) return new Res(false, "Ya existe una tablet registrada con el número de serie " + t.serie + ".");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("serie", t.serie); v.put("procesador", t.procesador); v.put("ram", t.ram);
            v.put("almacenamiento", t.almacenamiento); v.put("pantalla", t.pantalla);
            v.put("puertos", t.puertos); v.put("conectividad", t.conectividad);
            v.put("estado", Estado.EN_PANOL.name()); v.put("fecha_alta", ahora());
            db.insertOrThrow("tablet", null, v);
            mov(db, t.serie, "ALTA", null, Estado.EN_PANOL, 0, 0, null, "Alta en el pañol");
            db.setTransactionSuccessful();
            return new Res(true, "Tablet " + t.serie + " registrada en el Pañol.");
        } finally {
            db.endTransaction();
        }
    }

    private void mov(SQLiteDatabase db, String serie, String tipo, Estado o, Estado d, int legajo, long tarea,
                     String fisico, String obs) {
        ContentValues v = new ContentValues();
        v.put("serie", serie); v.put("tipo", tipo);
        v.put("estado_origen", o == null ? null : o.name());
        v.put("estado_destino", d.name());
        if (legajo != 0) v.put("legajo", legajo);
        if (tarea != 0) v.put("tarea_id", tarea);
        v.put("estado_fisico", fisico); v.put("observaciones", obs); v.put("fecha", ahora());
        v.put("uid", UUID.randomUUID().toString());
        db.insertOrThrow("movimiento", null, v);
    }

    private void cambiar(SQLiteDatabase db, String serie, Estado d, int legajo) {
        ContentValues v = new ContentValues();
        v.put("estado", d.name());
        if (legajo == 0) v.putNull("legajo_actual"); else v.put("legajo_actual", legajo);
        db.update("tablet", v, "serie=?", new String[]{serie});
    }

    /** CU-02: despacho. tareaId = 0 si no hay tarea asociada. */
    public Res despachar(String serie, int legajo, Reglas.Uso uso, long tareaId) {
        Tablet t = tablet(serie);
        if (t == null) return new Res(false, "La tablet no está registrada.");
        Empleado e = empleado(legajo);
        Tarea ta = tareaId == 0 ? null : tarea(tareaId);
        boolean tareaOk = ta != null && ta.activa && ta.legajo == legajo && ta.homeOffice;
        String err = Reglas.validarDespacho(t.estado, uso, e != null, tareaOk);
        if (err != null) return new Res(false, err);
        Estado dest = Reglas.destinoDespacho(uso);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            cambiar(db, t.serie, dest, legajo);
            mov(db, t.serie, "DESPACHO", t.estado, dest, legajo, tareaId, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return new Res(true, "Tablet " + t.serie + " entregada a " + e.nombre + " (" + dest.etiqueta + ").");
    }

    /** CU-03: devolución. */
    public Res devolver(String serie, boolean danada, String obs) {
        Tablet t = tablet(serie);
        if (t == null) return new Res(false, "La tablet no está registrada.");
        String err = Reglas.validarDevolucion(t.estado);
        if (err != null) return new Res(false, err);
        if (danada && (obs == null || obs.trim().isEmpty()))
            return new Res(false, "Si la tablet vuelve con daños, describa el daño en las observaciones.");
        Estado dest = Reglas.destinoDevolucion(danada);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            cambiar(db, t.serie, dest, 0);
            mov(db, t.serie, "DEVOLUCION", t.estado, dest, t.legajoActual, 0,
                    danada ? "CON_DANOS" : "BUEN_ESTADO", obs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return new Res(true, danada
                ? "Tablet " + t.serie + " recibida con daños: pasa a Servicio Técnico."
                : "Tablet " + t.serie + " recibida en buen estado: reingresa al Pañol.");
    }

    public Res finalizarServicio(String serie, String obs) {
        Tablet t = tablet(serie);
        if (t == null) return new Res(false, "La tablet no está registrada.");
        String err = Reglas.validarFinServicio(t.estado);
        if (err != null) return new Res(false, err);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            cambiar(db, t.serie, Estado.EN_PANOL, 0);
            mov(db, t.serie, "FIN_SERVICIO", t.estado, Estado.EN_PANOL, 0, 0, "BUEN_ESTADO", obs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return new Res(true, "Servicio técnico finalizado: " + t.serie + " vuelve al Pañol.");
    }

    // -------------------------------------------------------------- Empleados

    private static final String COLS_EMP = "legajo,nombre,area,activo";

    private Empleado leerEmp(Cursor c) {
        Empleado e = new Empleado();
        e.legajo = c.getInt(0); e.nombre = c.getString(1); e.area = c.getString(2); e.activo = c.getInt(3) == 1;
        return e;
    }

    /** Devuelve el empleado aunque esté desactivado (para mostrar nombres en historiales). */
    public Empleado empleado(int legajo) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS_EMP + " FROM empleado WHERE legajo=?",
                new String[]{String.valueOf(legajo)})) {
            return c.moveToFirst() ? leerEmp(c) : null;
        }
    }

    private List<Empleado> listaEmpleados(int activo) {
        List<Empleado> l = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS_EMP + " FROM empleado WHERE activo=? ORDER BY legajo",
                new String[]{String.valueOf(activo)})) {
            while (c.moveToNext()) l.add(leerEmp(c));
        }
        return l;
    }

    /** Solo empleados activos (los que pueden retirar tablets y recibir tareas). */
    public List<Empleado> empleados() { return listaEmpleados(1); }

    public List<Empleado> empleadosDesactivados() { return listaEmpleados(0); }

    public Res nuevoEmpleado(String legajo, String nombre, String area) {
        int l;
        try { l = Integer.parseInt(legajo.trim()); } catch (NumberFormatException ex) {
            return new Res(false, "El legajo debe ser un número.");
        }
        if (l <= 0) return new Res(false, "El legajo debe ser mayor que cero.");
        if (nombre.trim().isEmpty()) return new Res(false, "El nombre es obligatorio.");
        Empleado ex = empleado(l);
        if (ex != null) {
            return new Res(false, ex.activo ? "Ya existe el legajo " + l + "."
                    : "El legajo " + l + " pertenece a " + ex.nombre + ", que está desactivado. Reactivalo desde la lista.");
        }
        ContentValues v = new ContentValues();
        v.put("legajo", l); v.put("nombre", nombre.trim()); v.put("area", area.trim());
        getWritableDatabase().insert("empleado", null, v);
        return new Res(true, "Empleado " + nombre.trim() + " agregado.");
    }

    private int contar(String sql, String arg) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{arg})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /**
     * Eliminar empleado. Con tablet en su poder: no se puede. Sin historial: se borra. Con historial: se
     * desactiva (sus tareas activas se cierran) y su historial sigue consultable.
     */
    public Res eliminarEmpleado(int legajo) {
        Empleado e = empleado(legajo);
        if (e == null) return new Res(false, "El empleado no existe.");
        String l = String.valueOf(legajo);
        boolean tiene = contar("SELECT COUNT(*) FROM tablet WHERE legajo_actual=?", l) > 0;
        int movs = contar("SELECT COUNT(*) FROM movimiento WHERE legajo=?", l);
        Reglas.AccionEliminar acc = Reglas.accionEliminarEmpleado(tiene, movs);
        SQLiteDatabase db = getWritableDatabase();
        switch (acc) {
            case BLOQUEADO:
                return new Res(false, e.nombre + " tiene una tablet en su poder. Primero hay que registrar su devolución.");
            case BORRAR:
                db.beginTransaction();
                try {
                    db.delete("tarea", "legajo=?", new String[]{l});
                    db.delete("empleado", "legajo=?", new String[]{l});
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                return new Res(true, e.nombre + " fue eliminado (no tenía movimientos).");
            default:
                db.beginTransaction();
                try {
                    ContentValues v = new ContentValues(); v.put("activo", 0);
                    db.update("empleado", v, "legajo=?", new String[]{l});
                    ContentValues t = new ContentValues(); t.put("activa", 0);
                    db.update("tarea", t, "legajo=?", new String[]{l});
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                return new Res(true, e.nombre + " tiene historial, por eso quedó desactivado. Su historial se sigue pudiendo consultar.");
        }
    }

    public Res reactivarEmpleado(int legajo) {
        Empleado e = empleado(legajo);
        if (e == null) return new Res(false, "El empleado no existe.");
        ContentValues v = new ContentValues(); v.put("activo", 1);
        getWritableDatabase().update("empleado", v, "legajo=?", new String[]{String.valueOf(legajo)});
        return new Res(true, e.nombre + " fue reactivado.");
    }

    /** Historial de trabajos del empleado: cada retiro con su devolución (si ya la hubo), del más nuevo al más viejo. */
    public List<Prestamo> prestamos(int legajo) {
        List<Prestamo> l = new ArrayList<>();
        String q = "SELECT d.serie, d.fecha, d.estado_destino, ta.descripcion, ta.home_office, "
                + "r.fecha, r.estado_fisico, r.observaciones "
                + "FROM movimiento d LEFT JOIN tarea ta ON ta.id=d.tarea_id "
                + "LEFT JOIN movimiento r ON r.id = (SELECT MIN(x.id) FROM movimiento x "
                + "WHERE x.serie=d.serie AND x.tipo='DEVOLUCION' AND x.id>d.id) "
                + "WHERE d.tipo='DESPACHO' AND d.legajo=? ORDER BY d.id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(q, new String[]{String.valueOf(legajo)})) {
            while (c.moveToNext()) {
                Prestamo p = new Prestamo();
                p.serie = c.getString(0); p.fechaRetiro = c.getString(1); p.uso = c.getString(2);
                p.tarea = c.getString(3); p.homeOffice = c.getInt(4) == 1;
                p.fechaDevolucion = c.getString(5); p.estadoFisico = c.getString(6); p.observaciones = c.getString(7);
                l.add(p);
            }
        }
        return l;
    }

    // ----------------------------------------------------------------- Tareas

    private Tarea leerTarea(Cursor c) {
        Tarea t = new Tarea();
        t.id = c.getLong(0); t.legajo = c.getInt(1); t.descripcion = c.getString(2);
        t.homeOffice = c.getInt(3) == 1; t.activa = c.getInt(4) == 1; t.fecha = c.getString(5);
        return t;
    }

    public Tarea tarea(long id) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,legajo,descripcion,home_office,activa,fecha FROM tarea WHERE id=?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? leerTarea(c) : null;
        }
    }

    public List<Tarea> tareasActivas(int legajo) {
        List<Tarea> l = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,legajo,descripcion,home_office,activa,fecha FROM tarea WHERE legajo=? AND activa=1 ORDER BY id",
                new String[]{String.valueOf(legajo)})) {
            while (c.moveToNext()) l.add(leerTarea(c));
        }
        return l;
    }

    public Res nuevaTarea(int legajo, String desc, boolean homeOffice) {
        if (empleado(legajo) == null) return new Res(false, "El empleado no existe.");
        if (desc.trim().isEmpty()) return new Res(false, "Describa la tarea.");
        ContentValues v = new ContentValues();
        v.put("legajo", legajo); v.put("descripcion", desc.trim());
        v.put("home_office", homeOffice ? 1 : 0); v.put("fecha", ahora());
        v.put("uid", UUID.randomUUID().toString());
        getWritableDatabase().insert("tarea", null, v);
        return new Res(true, "Tarea asignada.");
    }

    public void cerrarTarea(long id) {
        ContentValues v = new ContentValues();
        v.put("activa", 0);
        getWritableDatabase().update("tarea", v, "id=?", new String[]{String.valueOf(id)});
    }

    // ------------------------------------------------- Compartir datos entre celulares

    private static String col(Cursor c, int i) { return c.isNull(i) ? null : c.getString(i); }

    /** Foto completa de los datos de este celular (tablets, empleados, tareas y movimientos). */
    public Intercambio.Snapshot exportar() {
        Intercambio.Snapshot sn = new Intercambio.Snapshot();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT legajo,nombre,area,activo FROM empleado ORDER BY legajo", null)) {
            while (c.moveToNext()) {
                Intercambio.Emp e = new Intercambio.Emp();
                e.legajo = c.getInt(0); e.nombre = c.getString(1); e.area = c.isNull(2) ? "" : c.getString(2); e.activo = c.getInt(3) == 1;
                sn.empleados.add(e);
            }
        }
        try (Cursor c = db.rawQuery("SELECT uid,legajo,descripcion,home_office,activa,fecha FROM tarea ORDER BY id", null)) {
            while (c.moveToNext()) {
                Intercambio.Tar t = new Intercambio.Tar();
                t.uid = c.getString(0); t.legajo = c.getInt(1); t.descripcion = c.getString(2);
                t.homeOffice = c.getInt(3) == 1; t.activa = c.getInt(4) == 1; t.fecha = c.getString(5);
                sn.tareas.add(t);
            }
        }
        for (Tablet t : tablets()) {
            Intercambio.Tab b = new Intercambio.Tab();
            b.serie = t.serie; b.procesador = t.procesador; b.ram = nz(t.ram); b.almacenamiento = nz(t.almacenamiento);
            b.pantalla = nz(t.pantalla); b.puertos = nz(t.puertos); b.conectividad = nz(t.conectividad); b.fechaAlta = t.fechaAlta;
            sn.tablets.add(b);
        }
        try (Cursor c = db.rawQuery("SELECT m.uid,m.serie,m.tipo,m.estado_origen,m.estado_destino,m.legajo,t.uid,"
                + "m.estado_fisico,m.observaciones,m.fecha FROM movimiento m LEFT JOIN tarea t ON t.id=m.tarea_id "
                + "ORDER BY m.id", null)) {
            while (c.moveToNext()) {
                Intercambio.Mov m = new Intercambio.Mov();
                m.uid = c.getString(0); m.serie = c.getString(1); m.tipo = c.getString(2); m.origen = col(c, 3);
                m.destino = c.getString(4); m.legajo = c.isNull(5) ? 0 : c.getInt(5); m.tareaUid = col(c, 6);
                m.estadoFisico = col(c, 7); m.observaciones = col(c, 8); m.fecha = c.getString(9);
                sn.movimientos.add(m);
            }
        }
        return sn;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /** Cantidad de datos NUEVOS que traería el archivo (lo que no está todavía en este celular). */
    public int[] contarNuevos(Intercambio.Snapshot s) {
        SQLiteDatabase db = getReadableDatabase();
        int e = 0, t = 0, b = 0, m = 0;
        for (Intercambio.Emp x : s.empleados) if (empleado(x.legajo) == null) e++;
        for (Intercambio.Tar x : s.tareas) if (contar("SELECT COUNT(*) FROM tarea WHERE uid=?", x.uid) == 0) t++;
        for (Intercambio.Tab x : s.tablets) if (tablet(x.serie) == null) b++;
        for (Intercambio.Mov x : s.movimientos) if (contar("SELECT COUNT(*) FROM movimiento WHERE uid=?", x.uid) == 0) m++;
        return new int[]{b, e, t, m};
    }

    /**
     * Combina los datos recibidos con los de este celular (no borra nada):
     * agrega lo que falta, cierra tareas que el otro cerró y recalcula el estado de cada tablet con su historial
     * (manda el movimiento más reciente). Todo o nada: si algo falla no se cambia nada.
     */
    public Res importar(Intercambio.Snapshot s) {
        SQLiteDatabase db = getWritableDatabase();
        int nE = 0, nT = 0, nB = 0, nM = 0, omitidos = 0;
        java.util.Set<String> afectadas = new java.util.HashSet<>();
        db.beginTransaction();
        try {
            for (Intercambio.Emp x : s.empleados) {
                Empleado ex = empleado(x.legajo);
                if (ex == null) {
                    ContentValues v = new ContentValues();
                    v.put("legajo", x.legajo); v.put("nombre", x.nombre); v.put("area", x.area); v.put("activo", x.activo ? 1 : 0);
                    db.insertOrThrow("empleado", null, v);
                    nE++;
                } else if (!x.activo && ex.activo
                        && contar("SELECT COUNT(*) FROM tablet WHERE legajo_actual=?", String.valueOf(x.legajo)) == 0) {
                    ContentValues v = new ContentValues(); v.put("activo", 0);
                    db.update("empleado", v, "legajo=?", new String[]{String.valueOf(x.legajo)});
                }
            }
            for (Intercambio.Tab x : s.tablets) {
                if (tablet(x.serie) != null) continue;
                ContentValues v = new ContentValues();
                v.put("serie", x.serie); v.put("procesador", x.procesador); v.put("ram", x.ram);
                v.put("almacenamiento", x.almacenamiento); v.put("pantalla", x.pantalla); v.put("puertos", x.puertos);
                v.put("conectividad", x.conectividad); v.put("estado", Estado.EN_PANOL.name()); v.put("fecha_alta", x.fechaAlta);
                db.insertOrThrow("tablet", null, v);
                afectadas.add(x.serie);
                nB++;
            }
            for (Intercambio.Tar x : s.tareas) {
                if (contar("SELECT COUNT(*) FROM tarea WHERE uid=?", x.uid) > 0) {
                    if (!x.activa) { // si en cualquiera de los dos celulares se cerró la tarea, queda cerrada
                        ContentValues v = new ContentValues(); v.put("activa", 0);
                        db.update("tarea", v, "uid=?", new String[]{x.uid});
                    }
                    continue;
                }
                if (empleado(x.legajo) == null) { omitidos++; continue; }
                ContentValues v = new ContentValues();
                v.put("uid", x.uid); v.put("legajo", x.legajo); v.put("descripcion", x.descripcion);
                v.put("home_office", x.homeOffice ? 1 : 0); v.put("activa", x.activa ? 1 : 0); v.put("fecha", x.fecha);
                db.insertOrThrow("tarea", null, v);
                nT++;
            }
            for (Intercambio.Mov x : s.movimientos) {
                if (contar("SELECT COUNT(*) FROM movimiento WHERE uid=?", x.uid) > 0) continue;
                if (tablet(x.serie) == null || (x.legajo != 0 && empleado(x.legajo) == null)) { omitidos++; continue; }
                ContentValues v = new ContentValues();
                v.put("uid", x.uid); v.put("serie", x.serie); v.put("tipo", x.tipo);
                v.put("estado_origen", x.origen); v.put("estado_destino", x.destino);
                if (x.legajo != 0) v.put("legajo", x.legajo);
                if (x.tareaUid != null) {
                    try (Cursor c = db.rawQuery("SELECT id FROM tarea WHERE uid=?", new String[]{x.tareaUid})) {
                        if (c.moveToFirst()) v.put("tarea_id", c.getLong(0));
                    }
                }
                v.put("estado_fisico", x.estadoFisico); v.put("observaciones", x.observaciones); v.put("fecha", x.fecha);
                db.insertOrThrow("movimiento", null, v);
                afectadas.add(x.serie);
                nM++;
            }
            for (String serie : afectadas) recalcular(db, serie);
            db.setTransactionSuccessful();
        } catch (RuntimeException ex) {
            return new Res(false, "No se pudo importar (no se cambió nada): " + ex.getMessage());
        } finally {
            db.endTransaction();
        }
        String msg = "Se agregaron: " + nB + " tablets, " + nE + " empleados, " + nT + " tareas y " + nM + " movimientos."
                + (omitidos > 0 ? "\nSe omitieron " + omitidos + " registros incompletos." : "");
        return new Res(true, msg);
    }

    /** Estado actual de la tablet = destino de su movimiento más reciente. */
    private void recalcular(SQLiteDatabase db, String serie) {
        List<Intercambio.Mov> l = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT estado_destino,legajo,fecha FROM movimiento WHERE serie=? ORDER BY id",
                new String[]{serie})) {
            while (c.moveToNext()) {
                Intercambio.Mov m = new Intercambio.Mov();
                m.destino = c.getString(0); m.legajo = c.isNull(1) ? 0 : c.getInt(1); m.fecha = c.getString(2);
                l.add(m);
            }
        }
        Intercambio.Ultimo u = Intercambio.ultimoEstado(l);
        cambiar(db, serie, u.estado, u.legajo);
    }

    // -------------------------------------------------------------- Historial

    public List<Movimiento> historial(String serie, int limite) {
        List<Movimiento> l = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT m.fecha,m.tipo,m.estado_origen,m.estado_destino,m.estado_fisico,m.observaciones,"
                        + "e.nombre,t.descripcion FROM movimiento m "
                        + "LEFT JOIN empleado e ON e.legajo=m.legajo LEFT JOIN tarea t ON t.id=m.tarea_id "
                        + "WHERE m.serie=? ORDER BY m.id DESC LIMIT " + limite,
                new String[]{QrCodec.normalizarSerie(serie)})) {
            while (c.moveToNext()) {
                Movimiento m = new Movimiento();
                m.fecha = c.getString(0); m.tipo = c.getString(1);
                m.origen = c.getString(2); m.destino = c.getString(3);
                m.estadoFisico = c.getString(4); m.observaciones = c.getString(5);
                m.empleado = c.getString(6); m.tarea = c.getString(7);
                l.add(m);
            }
        }
        return l;
    }
}
