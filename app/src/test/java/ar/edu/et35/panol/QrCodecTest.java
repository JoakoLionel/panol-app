package ar.edu.et35.panol;

import static org.junit.Assert.*;
import org.junit.Test;

public class QrCodecTest {
    @Test public void json_roundtrip() {
        Tablet t = new Tablet();
        t.serie = "tab-0001"; t.procesador = "Snapdragon 680"; t.ram = "4 GB"; t.almacenamiento = "64 GB";
        t.pantalla = "10.1\""; t.puertos = "1x USB-C, 1x Jack 3.5"; t.conectividad = "Wi-Fi, 4G, Bluetooth";
        Tablet r = QrCodec.parse(QrCodec.toJson(t));
        assertEquals("TAB-0001", r.serie);
        assertEquals("Snapdragon 680", r.procesador);
        assertEquals("10.1\"", r.pantalla);
        assertEquals("1x USB-C, 1x Jack 3.5", r.puertos);
        assertEquals("Wi-Fi, 4G, Bluetooth", r.conectividad);
    }
    @Test public void delimitado() {
        Tablet r = QrCodec.parse("TAB-0002;Helio G80;3 GB;32 GB;8\";1x USB-C;Wi-Fi, Bluetooth");
        assertEquals("TAB-0002", r.serie);
        assertEquals("3 GB", r.ram);
        assertEquals("Wi-Fi, Bluetooth", r.conectividad);
    }
    @Test public void delimitadoConPipe() {
        assertEquals("X1", QrCodec.parse("x1|a|b|c|d|e|f").serie);
    }
    @Test(expected = IllegalArgumentException.class) public void vacio() { QrCodec.parse("  "); }
    @Test(expected = IllegalArgumentException.class) public void jsonRoto() { QrCodec.parse("{\"sn\":\"A\""); }
    @Test(expected = IllegalArgumentException.class) public void sinSerie() { QrCodec.parse("{\"cpu\":\"x\"}"); }
    @Test(expected = IllegalArgumentException.class) public void camposFaltantes() { QrCodec.parse("A;B;C"); }
    @Test(expected = IllegalArgumentException.class) public void textoLibre() { QrCodec.parse("https://ejemplo.com"); }
}
