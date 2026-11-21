package ar.edu.et35.panol;

public final class Modelo {
    private Modelo() {}

    public static class Empleado {
        public int legajo; public String nombre; public String area; public boolean activo = true;
        @Override public String toString() { return legajo + " - " + nombre; }
    }

    public static class Tarea {
        public long id; public int legajo; public String descripcion;
        public boolean homeOffice; public boolean activa; public String fecha;
        @Override public String toString() {
            return descripcion + (homeOffice ? "  [Home Office autorizado]" : "  [solo en sitio]");
        }
    }

    public static class Movimiento {
        public String fecha, tipo, origen, destino, estadoFisico, observaciones, empleado, tarea;
    }

    /** Un retiro de tablet y, si ya ocurrió, su devolución. */
    public static class Prestamo {
        public String serie, uso, tarea, fechaRetiro, fechaDevolucion, estadoFisico, observaciones;
        public boolean homeOffice;
        public boolean devuelta() { return fechaDevolucion != null; }
    }

    public static class Res {
        public final boolean ok; public final String msg;
        public Res(boolean ok, String msg) { this.ok = ok; this.msg = msg; }
    }
}
