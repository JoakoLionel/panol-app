package ar.edu.et35.panol;

import android.content.Context;
import android.content.SharedPreferences;

/** Temas de color de la app (se elige desde el menú principal y se recuerda). */
public final class Tema {
    private Tema() {}

    public static final String[] NOMBRES = {"Claro azul", "Claro menta", "Claro violeta", "Claro naranja", "Negro"};
    public static final int NEGRO = 4;

    public static int actual = 0;
    /** Sube cada vez que se cambia el tema, para que las pantallas abiertas se redibujen. */
    public static int version = 0;

    // cabecera, primario, secundario, botón gris, título, texto, texto 2, fondo, tarjeta, campo de texto
    private static final int[][] PALETAS = {
        {0xFF1F3A5F, 0xFF1F3A5F, 0xFF1F7A8C, 0xFF5F6B7A, 0xFF1F3A5F, 0xFF1B1F24, 0xFF5F6B7A, 0xFFF3F5F8, 0xFFFFFFFF, 0xFFFFFFFF},
        {0xFF1E6F5C, 0xFF1E6F5C, 0xFF2A8C82, 0xFF5E7570, 0xFF1E6F5C, 0xFF1B1F24, 0xFF5E7570, 0xFFEAF6F1, 0xFFFFFFFF, 0xFFFFFFFF},
        {0xFF5B4B9A, 0xFF5B4B9A, 0xFF7E6BC4, 0xFF6B6783, 0xFF5B4B9A, 0xFF1B1F24, 0xFF6B6783, 0xFFF3F0FA, 0xFFFFFFFF, 0xFFFFFFFF},
        {0xFFB8541A, 0xFFB8541A, 0xFFC9731F, 0xFF7A6A5F, 0xFFB8541A, 0xFF1B1F24, 0xFF7A6A5F, 0xFFFFF5EC, 0xFFFFFFFF, 0xFFFFFFFF},
        {0xFF000000, 0xFF2F5D9B, 0xFF23808F, 0xFF3A3F46, 0xFF8FB9F2, 0xFFECECEC, 0xFFA3ABB5, 0xFF000000, 0xFF161616, 0xFF242424},
    };

    public static boolean oscuro() { return actual == NEGRO; }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("panol_prefs", Context.MODE_PRIVATE);
    }

    /** Lee el tema guardado y lo aplica a los colores de Ui. */
    public static void cargar(Context c) {
        int i = prefs(c).getInt("tema", 0);
        aplicar(i < 0 || i >= PALETAS.length ? 0 : i);
    }

    public static void guardar(Context c, int i) {
        prefs(c).edit().putInt("tema", i).apply();
        if (i != actual) version++;
        aplicar(i);
    }

    private static void aplicar(int i) {
        actual = i;
        int[] p = PALETAS[i];
        Ui.CABECERA = p[0]; Ui.PRIMARIO = p[1]; Ui.SECUNDARIO = p[2]; Ui.BTN_GRIS = p[3]; Ui.TITULO = p[4];
        Ui.TEXTO = p[5]; Ui.TEXTO2 = p[6]; Ui.FONDO = p[7]; Ui.TARJETA = p[8]; Ui.INPUT = p[9];
    }
}
