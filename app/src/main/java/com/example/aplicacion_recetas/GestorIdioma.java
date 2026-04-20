package com.example.aplicacion_recetas;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class GestorIdioma {
    private static final String PREFS = "config";
    private static final String KEY_LANG = "lang";

    // guardar el idioma seleccionado em sharedPreferences
    public static void setIdioma(Activity activity, String lang) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    // aplica el idioma guardado en la configuración de la app, se ejecuta al iniciar cada activity
    public static void aplicarIdioma(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // coge el idioma guardado o usa el idioma por defecto
        String lang = prefs.getString(KEY_LANG, Locale.getDefault().getLanguage());

        // crea el objeto Locale con el idioma seleccionado
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        // configura el sistema con el nuevo idioma
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);

        // ajusta la dirección del texto
        config.setLayoutDirection(locale);

        // aplica la config al sistema de recursos
        base.getResources().updateConfiguration(
                config,
                base.getResources().getDisplayMetrics()
        );
    }
}
