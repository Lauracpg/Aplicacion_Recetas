package com.example.aplicacion_recetas;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class RecetasWorker extends Worker {
    public RecetasWorker(@NonNull Context context,
                         @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        try {
            // acción solicitada
            String accion = getInputData().getString("accion");

            URL url = new URL("http://34.175.70.22:81/recetas.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            // formato de envío por POST
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("accion", accion);

            // sesión del usuario
            GestorSesionUsuario sesion =
                    new GestorSesionUsuario(getApplicationContext());
            int idUsuario = sesion.getUserId();

            // construcción de parámetros según la acción
            if ("insertar".equals(accion)) {
                builder.appendQueryParameter("titulo", getInputData().getString("titulo"))
                        .appendQueryParameter("categoria", getInputData().getString("categoria"))
                        .appendQueryParameter("tiempo", String.valueOf(getInputData().getInt("tiempo", 0)))
                        .appendQueryParameter("ingredientes", getInputData().getString("ingredientes"))
                        .appendQueryParameter("pasos", getInputData().getString("pasos"))
                        .appendQueryParameter("fotoUri", getInputData().getString("fotoUri"))
                        .appendQueryParameter("idUsuario", String.valueOf(getInputData().getInt("idUsuario", 0)));

            } else if("obtener".equals(accion)) {
                builder.appendQueryParameter("idUsuario", String.valueOf(idUsuario));

            } else if ("eliminar".equals(accion)) {
                builder.appendQueryParameter("id",
                                String.valueOf(getInputData().getInt("id", -1)))
                        .appendQueryParameter("idUsuario",
                                String.valueOf(idUsuario));

            } else if ("favorito".equals(accion)) {
                builder.appendQueryParameter("id",
                                String.valueOf(getInputData().getInt("id", 0)))
                        .appendQueryParameter("idUsuario",
                                String.valueOf(idUsuario))
                        .appendQueryParameter("favorita",
                                String.valueOf(getInputData().getInt("favorita", 0)));
            }

            // convierte los parámetros a formato URL encoded
            String params = builder.build().getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));

            writer.write(params);
            writer.flush();
            writer.close();
            os.close();

            // leer la respuesta del servidor
            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();

            // si acción es obtener, actualiza datos del widget
            if ("obtener".equals(accion)) {
                SharedPreferences prefs =
                        getApplicationContext().getSharedPreferences("widget_data", Context.MODE_PRIVATE);

                prefs.edit()
                        .putString("recetas_json", json)
                        .apply();

                actualizarWidget(getApplicationContext());
            }

            // resultado exitoso del worker
            return Result.success(
                    new Data.Builder()
                            .putString("response", json)
                            .putString("accion", accion)
                            .build()
            );

        } catch (Exception e) {
            e.printStackTrace();
            // resultado fallido con mensaje de error
            return Result.failure(
                    new Data.Builder()
                            .putString("error", e.getMessage())
                            .build()
            );
        }
    }

    // forzar la actualización del widget
    private void actualizarWidget(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, WidgetReceta.class);

        int[] ids = manager.getAppWidgetIds(widget);

        Intent intent = new Intent(context, WidgetReceta.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        context.sendBroadcast(intent);
    }
}