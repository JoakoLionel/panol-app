package ar.edu.et35.panol;

/** Estados exclusivos del ciclo de vida de una tablet. */
public enum Estado {
    EN_PANOL("En Pañol", 0xFF2E7D32),
    USO_LOCAL("En Uso Local (Oficina)", 0xFF1565C0),
    USO_EXTERNO("En Uso Externo (Home Office)", 0xFFEF6C00),
    SERVICIO_TECNICO("En Servicio Técnico", 0xFFC62828);

    public final String etiqueta;
    public final int color;

    Estado(String etiqueta, int color) {
        this.etiqueta = etiqueta;
        this.color = color;
    }

    public boolean enUso() {
        return this == USO_LOCAL || this == USO_EXTERNO;
    }
}
