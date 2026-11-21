package ar.edu.et35.panol;

/** Reglas de negocio puras (sin Android) para poder probarlas con JUnit. */
public final class Reglas {
    private Reglas() {}

    public enum Uso { LOCAL, EXTERNO }

    /** @return null si se puede despachar; si no, el motivo del rechazo. */
    public static String validarDespacho(Estado actual, Uso uso, boolean hayEmpleado, boolean tareaHomeOfficeVigente) {
        if (actual != Estado.EN_PANOL) {
            return "La tablet no está disponible: su estado actual es \"" + actual.etiqueta
                    + "\". Solo se puede despachar una tablet que esté En Pañol.";
        }
        if (!hayEmpleado) {
            return "Debe asociar la tablet al legajo de un empleado.";
        }
        if (uso == Uso.EXTERNO && !tareaHomeOfficeVigente) {
            return "ALERTA: el empleado no tiene una tarea activa autorizada para Home Office que justifique "
                    + "llevarse la tablet a su casa. Asigne o habilite la tarea antes de despachar.";
        }
        return null;
    }

    public static Estado destinoDespacho(Uso uso) {
        return uso == Uso.EXTERNO ? Estado.USO_EXTERNO : Estado.USO_LOCAL;
    }

    public static String validarDevolucion(Estado actual) {
        if (!actual.enUso()) {
            return "No hay nada que devolver: la tablet figura \"" + actual.etiqueta + "\".";
        }
        return null;
    }

    /** Si vuelve dañada va a Servicio Técnico; si no, reingresa al Pañol. */
    public static Estado destinoDevolucion(boolean danada) {
        return danada ? Estado.SERVICIO_TECNICO : Estado.EN_PANOL;
    }

    public static String validarFinServicio(Estado actual) {
        if (actual != Estado.SERVICIO_TECNICO) {
            return "La tablet no está en Servicio Técnico (estado actual: " + actual.etiqueta + ").";
        }
        return null;
    }

    public enum AccionEliminar { BLOQUEADO, BORRAR, DESACTIVAR }

    /**
     * Eliminar un empleado: si tiene una tablet en su poder no se puede; si nunca tuvo movimientos se borra
     * del todo; si tiene historial se desactiva (para conservar su historial).
     */
    public static AccionEliminar accionEliminarEmpleado(boolean tieneTablet, int movimientos) {
        if (tieneTablet) return AccionEliminar.BLOQUEADO;
        return movimientos == 0 ? AccionEliminar.BORRAR : AccionEliminar.DESACTIVAR;
    }
}
