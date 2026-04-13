package com.example.aplicacion_recetas;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class GestorIdioma {
    private static final String PREFS = "config";
    private static final String KEY_LANG = "lang";

    public static boolean languageChanged = false;
    public static void setIdioma(Activity activity, String lang) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();

        languageChanged = true;
    }

    public static void aplicarIdioma(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANG, Locale.getDefault().getLanguage());

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        base.getResources().updateConfiguration(
                config,
                base.getResources().getDisplayMetrics()
        );
    }
}
