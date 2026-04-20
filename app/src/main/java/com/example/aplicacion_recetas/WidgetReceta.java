package com.example.aplicacion_recetas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;

public class WidgetReceta extends AppWidgetProvider{
    // se ejecuta cuando el widget necesita actualizarse
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for(int appWidgetId : appWidgetIds) {
            actualizarWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // construye y actualiza el widget
    static void actualizarWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // layout del widget
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_receta
        );

        // obtener la última respuesta guardada por el worker
        SharedPreferences prefs =
                context.getSharedPreferences("widget_data", Context.MODE_PRIVATE);

        String json = prefs.getString("recetas_json", null);

        int recetaId = -1;

        try {
            // si hay datos almacenados
            if (json != null && !json.isEmpty()) {

                JSONObject obj = new JSONObject(json);

                if (obj.optBoolean("success")) {

                    JSONArray recetas = obj.optJSONArray("recetas");

                    // si hay recetas disponibles
                    if (recetas != null && recetas.length() > 0) {
                        // elegir una receta random para mostrar en el widget
                        JSONObject receta =
                                recetas.getJSONObject(new Random().nextInt(recetas.length()));

                        recetaId = receta.optInt("id", -1);

                        // rellenar los datos del widget
                        views.setTextViewText(R.id.txtTituloWidget,
                                receta.optString("titulo", "Sin título"));

                        views.setTextViewText(R.id.txtReceta,
                                receta.optString("categoria", ""));

                        views.setTextViewText(R.id.txtTiempo,
                                receta.optInt("tiempo", 0) + " min");

                    } else {
                        mostrarVacio(views);
                    }
                } else {
                    mostrarVacio(views);
                }
            } else {
                mostrarVacio(views);
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarVacio(views);
        }

        // Botón "Otra receta"
        Intent intent = new Intent(context, WidgetReceta.class);
        intent.setAction("FORCE_UPDATE");

        PendingIntent refreshIntent = PendingIntent.getBroadcast(
                context,
                2001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.btnActualizar, refreshIntent);

        // Click en el widget para abrir detalle receta
        if (recetaId != -1) {
            Intent openIntent = new Intent(context, DetalleRecetaActivity.class);
            openIntent.putExtra("receta_id", recetaId);

            PendingIntent openPendingIntent = PendingIntent.getActivity(
                    context,
                    4000 + appWidgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent);
        }
        // actualizar widget en pantalla
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // muestra estado vacío cuando no hay recetas
    private static void mostrarVacio(RemoteViews views) {
        views.setTextViewText(R.id.txtTituloWidget, "Sugerencia para hoy");
        views.setTextViewText(R.id.txtReceta, "");
        views.setTextViewText(R.id.txtTiempo, "--");
    }

    // se ejecuta cuando llega un broadcast (update o refresh manual)
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())
                || "FORCE_UPDATE".equals(intent.getAction())) {

            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, WidgetReceta.class);

            int[] ids = manager.getAppWidgetIds(widget);
            // actualiza widgets activos
            for (int id : ids) {
                actualizarWidget(context, manager, id);
            }
        }
    }

    // se ejecuta cuando se añade el primer widget a la pantalla
    @Override
    public void onEnabled(Context context) {
        // programa actualización periódica cada 30 min
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, WidgetAlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long interval = 30 * 60 * 1000; // 30 minutos
        alarmManager.setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + interval,
                interval,
                pendingIntent
        );
    }

    // se ejecuta cuando se elimina el último widget
    @Override
    public void onDisabled(Context context) {
        // cancela la actualización automática
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, WidgetAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }
}