package ar.edu.et35.panol;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Utilidades para armar la interfaz por código. */
public final class Ui {
    private Ui() {}

    // Colores del tema activo (los fija Tema.cargar)
    public static int CABECERA = 0xFF1F3A5F, PRIMARIO = 0xFF1F3A5F, SECUNDARIO = 0xFF1F7A8C, BTN_GRIS = 0xFF5F6B7A,
            TITULO = 0xFF1F3A5F, TEXTO = 0xFF1B1F24, TEXTO2 = 0xFF5F6B7A, FONDO = 0xFFF3F5F8,
            TARJETA = 0xFFFFFFFF, INPUT = 0xFFFFFFFF;

    public static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    public static TextView tv(Context c, String t, int sp, boolean bold, int color) {
        TextView v = new TextView(c);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    public static LinearLayout.LayoutParams lp(Context c, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(c, top);
        return p;
    }

    public static GradientDrawable fondo(Context c, int color, int radioDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(c, radioDp));
        return g;
    }

    public static Button boton(Context c, String t, int color, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(t); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setTextSize(16);
        b.setBackground(fondo(c, color, 10));
        b.setPadding(dp(c, 12), dp(c, 14), dp(c, 12), dp(c, 14));
        b.setOnClickListener(l);
        return b;
    }

    public static LinearLayout tarjeta(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(fondo(c, TARJETA, 12));
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        l.setElevation(dp(c, 2));
        return l;
    }

    public static TextView chip(Context c, String t, int color) {
        TextView v = tv(c, t, 14, true, Color.WHITE);
        v.setBackground(fondo(c, color, 20));
        v.setPadding(dp(c, 12), dp(c, 5), dp(c, 12), dp(c, 5));
        return v;
    }

    public static LinearLayout fila(Context c, String etiqueta, String valor) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        TextView a = tv(c, etiqueta, 14, false, TEXTO2);
        a.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.38f));
        TextView b = tv(c, valor == null || valor.isEmpty() ? "—" : valor, 15, true, TEXTO);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.62f));
        r.addView(a); r.addView(b);
        r.setPadding(0, dp(c, 5), 0, dp(c, 5));
        return r;
    }
}
