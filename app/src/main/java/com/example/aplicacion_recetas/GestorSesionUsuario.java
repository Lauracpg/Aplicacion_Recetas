package com.example.aplicacion_recetas;

import android.content.Context;
import android.content.SharedPreferences;

public class GestorSesionUsuario {
    private static final String PREFS_NAME = "sesion_usuario";
    private static final String KEY_ID = "id";
    private static final String KEY_NOMBRE = "nombre";

    private static final String KEY_EMAIL = "email";
    private static final String KEY_FOTO = "foto";

    private final SharedPreferences prefs; // donde se guarda la sesión localmente

    // inicializar el acceso a SharedPreferences
    public GestorSesionUsuario(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // guarda los datos del usuario tras iniciar sesión/registrarse
    public void guardarUsuario(int id, String nombre, String email) {
        prefs.edit()
                .putInt(KEY_ID, id)
                .putString(KEY_NOMBRE, nombre)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    // guarda la ruta de la foto de perfil del usuario
    public void guardarFoto(String ruta) {
        prefs.edit().putString(KEY_FOTO, ruta).apply();
    }

    // cierra la sesión del usuario eliminado los datos guardados
    public void cerrarSesion() {
        prefs.edit().clear().apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_ID, -1);
    }

    public String getUserName() {
        return prefs.getString(KEY_NOMBRE, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getFoto() {
        return prefs.getString(KEY_FOTO, null);
    }

    // comprueba si hay una sesión activa
    public boolean estaLogueado() {
        return getUserId() != -1;
    }
}