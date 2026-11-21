package ar.edu.et35.panol;

/** Ficha técnica de una tablet (lo que viaja codificado en el QR + su estado actual). */
public class Tablet {
    public String serie = "";
    public String procesador = "";
    public String ram = "";
    public String almacenamiento = "";
    public String pantalla = "";
    public String puertos = "";
    public String conectividad = "";
    public Estado estado = Estado.EN_PANOL;
    public int legajoActual = 0; // 0 = nadie la tiene
    public String fechaAlta = "";
}
